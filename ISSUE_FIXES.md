# Issue Fixes - Personal Diary Backend

## Critical Issues Found During Testing

### Issue #1: Entry Update Endpoint Returns 500 Error

**Location:** `/Users/jaystuart/dev/personal-diary/backend/app/routers/entries.py` Lines 278-293

**Problem:**
```python
# Line 278-282: Incorrect tag deletion
await db.execute(
    select(Tag).where(Tag.entry_id == entry.id)  # This just selects, doesn't delete!
)
for old_tag in entry.tags:
    await db.delete(old_tag)  # db.delete() doesn't exist on AsyncSession
```

**Root Cause:**
1. Line 278-280 executes a SELECT statement but doesn't use the result
2. Line 282 calls `await db.delete(old_tag)` which is not a valid AsyncSession method
3. Should use `await db.execute(delete(Tag).where(...))` instead

**Fix:**
```python
from sqlalchemy import delete

# Update tags if provided
if entry_data.tag_names is not None:
    changes["tags"] = True

    # Remove old tags using delete statement
    await db.execute(
        delete(Tag).where(Tag.entry_id == entry.id)
    )

    # Clear tags list
    entry.tags = []

    # Add new tags
    for tag_name in entry_data.tag_names:
        tag = Tag(
            entry_id=entry.id,
            tag_name=tag_name.lower().strip(),
            auto_generated=False,
        )
        db.add(tag)
        entry.tags.append(tag)
```

**Testing:**
```bash
# Test the fix
curl -X PUT "http://localhost:3001/api/v1/entries/{entry_id}" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "encrypted_content": "updated_content",
    "content_hash": "new_hash_64_chars_long_sha256_hash_here_exactly_64_chars_total",
    "tag_names": ["updated", "test"]
  }'
```

---

### Issue #2: Search Endpoint Returns 500 Error

**Location:** `/Users/jaystuart/dev/personal-diary/backend/app/routers/search.py`

**Problem:**
The search implementation assumes PostgreSQL-specific features:
- `ts_vector` column (full-text search vector)
- `plainto_tsquery()` function
- `ts_rank()` function
- `@@` text search operator

But the development environment uses **SQLite**, which doesn't support these features.

**Root Cause:**
```python
# Lines 66-72: PostgreSQL-specific code
search_query = func.plainto_tsquery("english", query.query)
stmt = stmt.where(Entry.search_vector.op("@@")(search_query))
rank = func.ts_rank(Entry.search_vector, search_query).label("rank")
```

This fails in SQLite with:
- No `search_vector` column exists (it's PostgreSQL-only)
- SQLite doesn't have `plainto_tsquery` or `ts_rank` functions

**Immediate Fix (SQLite Compatible):**

Replace the search logic with SQLite-compatible LIKE queries:

```python
# Apply text search if query provided
if query.query.strip():
    # Use LIKE for SQLite (simpler but less powerful than PostgreSQL FTS)
    search_terms = query.query.split()
    search_conditions = []

    for term in search_terms:
        # Search in encrypted_content (metadata) or tags
        # Note: Full-text search on encrypted content won't work well
        # This is a limitation - UCE should really use PostgreSQL
        search_conditions.append(
            Entry.encrypted_content.contains(term)
        )

    if search_conditions:
        stmt = stmt.where(or_(*search_conditions))

    # Simple relevance: just use creation date
    stmt = stmt.add_columns(func.cast(1.0, type_=type(1.0)).label("rank"))
    stmt = stmt.order_by(Entry.created_at.desc())
else:
    # No search query, just filter and sort by date
    stmt = stmt.add_columns(func.cast(1.0, type_=type(1.0)).label("rank"))
    stmt = stmt.order_by(Entry.created_at.desc())
```

**Better Fix (For Production):**

1. **Use PostgreSQL in production** - Full-text search is a core feature
2. **Add database type detection**:

```python
from app.config import settings

def is_postgres():
    return 'postgresql' in settings.DATABASE_URL

@router.post("/", ...)
async def search_entries(...):
    if not current_user.is_uce:
        raise HTTPException(...)

    try:
        if is_postgres():
            # Use PostgreSQL full-text search
            results = await _search_postgres(db, current_user, query)
        else:
            # Use SQLite simple search
            results = await _search_sqlite(db, current_user, query)

        return results
    except Exception as e:
        logger.error(f"Search error: {str(e)}")
        raise HTTPException(...)

async def _search_postgres(db, user, query):
    """PostgreSQL full-text search implementation"""
    # ... existing code with ts_vector ...

async def _search_sqlite(db, user, query):
    """SQLite simple search implementation"""
    # ... LIKE-based search ...
```

**Search Stats Fix:**

Similarly, fix the `search_vector` references in `/stats`:

```python
@router.get("/stats", ...)
async def get_search_stats(...):
    try:
        # Get total entries
        total_stmt = select(func.count()).where(
            Entry.user_id == current_user.id,
            Entry.deleted_at.is_(None)
        )
        total_result = await db.execute(total_stmt)
        total_entries = total_result.scalar()

        # For SQLite, all entries are "searchable" (no search_vector column)
        # For PostgreSQL, check search_vector is not null
        if is_postgres():
            searchable_stmt = select(func.count()).where(
                Entry.user_id == current_user.id,
                Entry.deleted_at.is_(None),
                Entry.search_vector.isnot(None),
            )
        else:
            # SQLite: same as total
            searchable_stmt = total_stmt

        searchable_result = await db.execute(searchable_stmt)
        searchable_entries = searchable_result.scalar()

        # Get last updated entry
        last_indexed_stmt = (
            select(Entry.updated_at)
            .where(Entry.user_id == current_user.id)
            .order_by(Entry.updated_at.desc())
            .limit(1)
        )
        last_indexed_result = await db.execute(last_indexed_stmt)
        last_indexed_at = last_indexed_result.scalar_one_or_none()

        return SearchStatsResponse(
            total_entries=total_entries,
            searchable_entries=searchable_entries,
            last_indexed_at=last_indexed_at,
            index_size_bytes=None,
        )
    except Exception as e:
        logger.error(f"Get search stats error: {str(e)}")
        raise HTTPException(...)
```

---

### Issue #3: Search Implementation Limitation

**Problem:**
The current search implementation has a fundamental issue:
- UCE entries are **encrypted** before storage
- The encrypted content cannot be full-text searched meaningfully
- PostgreSQL FTS would need **plaintext** content to index

**Current Architecture Issue:**
```
User creates entry:
1. Client encrypts content → "U2FsdGVkX1+abc123..."
2. Sends encrypted content to backend
3. Backend stores encrypted content
4. Backend tries to search encrypted content ❌ (impossible!)
```

**Correct UCE Architecture Should Be:**
```
User creates entry (UCE tier):
1. Client encrypts content with user password
2. Sends encrypted content + plaintext (for server) to backend
3. Backend re-encrypts plaintext with server key
4. Backend stores:
   - encrypted_content (for client)
   - server_encrypted_content (for server search)
5. Backend indexes server_encrypted_content
6. Search works on server_encrypted_content
```

**Current Workaround:**
For SQLite development, search will be limited to:
- Metadata (tags, mood, date)
- Entry titles (if stored separately)
- Basic pattern matching (limited)

**Recommendation:**
1. Document this limitation clearly
2. Implement proper dual-encryption for UCE in production
3. For now, rely on client-side search for development

---

## Implementation Priority

### Immediate (Critical - Fix Now)
1. ✅ Fix entry update endpoint (tag deletion issue)
   - Status: Fix ready, needs implementation
   - Impact: HIGH - Users cannot edit entries

### High Priority (This Week)
2. ⚠️ Fix search endpoint for SQLite
   - Status: Fix designed, needs implementation
   - Impact: MEDIUM - Search broken in dev environment
   - Note: Will work differently than production

3. ⚠️ Fix search stats endpoint
   - Status: Fix designed, needs implementation
   - Impact: LOW - Stats are informational only

### Medium Priority (This Month)
4. 📋 Document search limitations in README
5. 📋 Add database type detection utility
6. 📋 Implement separate search functions for PostgreSQL vs SQLite
7. 📋 Add migration guide for PostgreSQL setup

### Long-term (Future)
8. 🔄 Redesign UCE encryption for proper server-side search
9. 🔄 Implement WebSocket-based search for real-time results
10. 🔄 Add search result caching

---

## Testing After Fixes

### Test Entry Update
```python
import requests

BASE_URL = "http://localhost:3001/api/v1"

# Login to get token
response = requests.post(f"{BASE_URL}/auth/login", json={
    "email": "test@example.com",
    "password": "password123"
})
token = response.json()["tokens"]["accessToken"]
headers = {"Authorization": f"Bearer {token}"}

# Create entry
entry_response = requests.post(f"{BASE_URL}/entries/", headers=headers, json={
    "encrypted_content": "original_content",
    "content_hash": "a" * 64,
    "tag_names": ["original", "test"]
})
entry_id = entry_response.json()["id"]

# Update entry (THIS SHOULD NOW WORK)
update_response = requests.put(
    f"{BASE_URL}/entries/{entry_id}",
    headers=headers,
    json={
        "encrypted_content": "updated_content",
        "content_hash": "b" * 64,
        "tag_names": ["updated", "test", "working"]
    }
)

print(f"Update Status: {update_response.status_code}")
print(f"Response: {update_response.json()}")

# Expected: 200 OK with updated entry
assert update_response.status_code == 200
assert len(update_response.json()["tags"]) == 3
```

### Test Search (After SQLite Fix)
```python
# Search entries
search_response = requests.post(
    f"{BASE_URL}/search/",
    headers=headers,
    json={"query": "test", "page": 1, "page_size": 10}
)

print(f"Search Status: {search_response.status_code}")
print(f"Results: {search_response.json()}")

# Expected: 200 OK with search results
assert search_response.status_code == 200
```

### Test Search Stats (After Fix)
```python
# Get search stats
stats_response = requests.get(f"{BASE_URL}/search/stats", headers=headers)

print(f"Stats Status: {stats_response.status_code}")
print(f"Stats: {stats_response.json()}")

# Expected: 200 OK with statistics
assert stats_response.status_code == 200
```

---

## Summary

**Issues Found:** 3 critical issues
**Fixes Ready:** 3
**Implementation Time:** ~1-2 hours
**Testing Time:** 30 minutes

**Files to Modify:**
1. `/backend/app/routers/entries.py` - Fix tag deletion (lines 274-293)
2. `/backend/app/routers/search.py` - Add SQLite compatibility (entire file)
3. `/backend/app/config.py` - Add database type detection helper (optional)

**After Fixes:**
- Entry update will work ✅
- Search will work (limited functionality in SQLite) ✅
- Search stats will work ✅
- All core features functional ✅

**Remaining Limitations:**
- Search quality limited in SQLite (expected for dev environment)
- Should use PostgreSQL for production deployment
- UCE search architecture needs improvement for production
