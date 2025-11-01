# Search Implementation Specification - Personal Diary Platform

**Document Version:** 1.0  
**Last Updated:** October 31, 2025  
**Target:** AI Agent Implementation  
**Purpose:** Dual-tier search architecture

---

## 📋 Overview

This document specifies the search implementation for both encryption tiers. The search strategy differs significantly between E2E and UCE tiers due to encryption constraints.

**Key Distinction:**
- **UCE Tier:** Server-side full-text search (server can decrypt)
- **E2E Tier:** Client-side search only (server cannot decrypt)

---

## 🎯 Requirements Summary

### UCE Tier Search Requirements

**Capabilities:**
- Full-text search on decrypted content
- Keyword matching
- Phrase search ("exact phrase")
- Tag filtering
- Date range filtering
- Source filtering (diary/facebook/instagram)
- Relevance ranking
- Fuzzy matching (typo tolerance)
- Search suggestions/autocomplete

**Performance:**
- Response time < 200ms (95th percentile)
- Handle up to 100,000 entries per user
- Concurrent searches (multiple users)

**Security:**
- Search index encrypted at rest
- User isolation (cannot search other users' entries)
- Audit logging (optional)

---

### E2E Tier Search Requirements

**Capabilities:**
- Local search on client device
- Keyword matching
- Tag filtering
- Date range filtering
- Source filtering
- Limited results (based on local storage)

**Limitations:**
- No server-side search
- No cross-device search history
- No search suggestions from server
- Slower for large datasets (10,000+ entries)

**Benefits:**
- Works offline
- Maximum privacy
- No search queries sent to server

---

## 🔍 UCE Server-Side Search

### Technology Options

Agent must choose one approach:

#### Option A: PostgreSQL Full-Text Search (Recommended for MVP)

**Pros:**
- Built into PostgreSQL
- No additional services
- Simple setup
- Good for MVP
- Cost-effective

**Cons:**
- Limited advanced features
- Less scalable than dedicated search
- Basic relevance ranking

**When to Use:**
- MVP with < 100,000 entries per user
- Simple search requirements
- Limited infrastructure budget

---

#### Option B: Elasticsearch/OpenSearch (Recommended for Scale)

**Pros:**
- Dedicated search engine
- Advanced features (fuzzy search, suggestions)
- Better relevance ranking
- Highly scalable
- Real-time indexing

**Cons:**
- Additional service to manage
- More complex setup
- Higher infrastructure cost

**When to Use:**
- Need advanced search features
- Scaling beyond 100,000 entries per user
- Multiple languages/analyzers
- Complex relevance tuning

---

#### Option C: Typesense (Alternative)

**Pros:**
- Lightweight, fast
- Easier than Elasticsearch
- Good relevance out of box
- Typo tolerance built-in

**Cons:**
- Smaller ecosystem
- Fewer integrations
- Less mature than Elasticsearch

**When to Use:**
- Middle ground between PostgreSQL and Elasticsearch
- Want simplicity with good features

---

### Agent Decision

**Agent should:**
1. Start with **PostgreSQL Full-Text Search** for MVP
2. Design abstraction layer for future migration
3. Monitor performance metrics
4. Plan migration to Elasticsearch when:
   - Users exceed 50,000 entries
   - Search performance degrades
   - Advanced features needed

---

## 📊 PostgreSQL Full-Text Search Implementation

### Architecture

```
Client Request: "search vacation"
    ↓
API Endpoint: GET /api/v1/search?q=vacation
    ↓
Auth Middleware (verify JWT)
    ↓
Search Service
    ↓
Check encryption tier → UCE
    ↓
Decrypt entries using user's master key
    ↓
PostgreSQL Full-Text Search
    ↓
Rank results by relevance
    ↓
Return encrypted entries + metadata
    ↓
Client decrypts for display
```

### Database Schema Requirements

**Agent must add to Entry table:**

**New Columns:**
- `search_vector` (tsvector) - Full-text search vector
- `search_content` (text) - Decrypted content (temporary, for indexing)

**Indexes:**
- GIN index on `search_vector`
- Partial index (only for UCE users)

**Note:** The `search_content` column should only exist for UCE users, or be null for E2E users.

---

### Indexing Process

**When entry is created/updated (UCE tier):**

1. Server decrypts entry content using user's master key
2. Generate tsvector from decrypted content
3. Store tsvector in `search_vector` column
4. Optionally store decrypted text in `search_content` (if needed for highlighting)
5. Both columns encrypted at rest (database-level encryption)

**SQL Example (Agent implements):**

```
-- Create tsvector column
ALTER TABLE entries 
ADD COLUMN search_vector tsvector;

-- Create GIN index
CREATE INDEX entries_search_idx 
ON entries 
USING GIN (search_vector);

-- Create trigger to auto-update search_vector
-- (Agent implements trigger function)
```

---

### Search Query Implementation

**Search Types:**

1. **Simple Keyword Search:**
   - Query: "vacation"
   - Matches: entries containing "vacation"

2. **Multiple Keywords (AND):**
   - Query: "beach vacation"
   - Matches: entries containing both "beach" AND "vacation"

3. **Phrase Search:**
   - Query: "family trip to Hawaii"
   - Matches: exact phrase

4. **Prefix Search:**
   - Query: "vac*"
   - Matches: vacation, vacate, etc.

**Agent implements search service with:**

```
Function: searchEntries(userId, query, filters)

Parameters:
- userId: string (from JWT)
- query: string (search keywords)
- filters: object {
    tags: string[] (optional)
    startDate: ISO date (optional)
    endDate: ISO date (optional)
    source: enum (optional)
  }

Returns:
- entries: Entry[] (matching entries)
- total: number (total matches)
- page: number
- perPage: number
```

---

### Relevance Ranking

**PostgreSQL ts_rank:**

Agent should use `ts_rank()` or `ts_rank_cd()` for relevance scoring.

**Factors:**
- Term frequency (how often search term appears)
- Document length (shorter docs ranked higher)
- Term proximity (for phrase searches)
- Boost recent entries (optional)

**Example ranking query (Agent implements):**

```sql
-- Example for reference (not actual code to include)
SELECT entry_id, 
       ts_rank(search_vector, query) as rank
FROM entries
WHERE search_vector @@ query
  AND user_id = ?
ORDER BY rank DESC
LIMIT 20;
```

---

### Performance Optimization

**Agent must ensure:**

1. **Index Maintenance:**
   - Auto-update search_vector on entry changes
   - Rebuild indexes periodically if needed

2. **Query Optimization:**
   - Use EXPLAIN ANALYZE to check query plans
   - Ensure indexes are being used
   - Limit result sets (pagination)

3. **Caching (Optional):**
   - Cache search results in Redis (short TTL)
   - Cache common queries

4. **Monitoring:**
   - Track slow queries (> 200ms)
   - Monitor index size
   - Alert on performance degradation

---

## 🔎 E2E Client-Side Search

### Architecture

```
User types search: "vacation"
    ↓
Client Search Service
    ↓
Query local IndexedDB/SQLite/Room
    ↓
Decrypt matching entries
    ↓
Filter and rank locally
    ↓
Display results
```

### Platform-Specific Implementation

#### iOS Implementation

**Technology:**
- Core Data with NSPredicate
- Or SQLite with FTS5 (Full-Text Search extension)
- Or in-memory search for small datasets

**Approach:**

1. **Download all entries to device**
   - On sync, store encrypted entries locally
   - Maintain local decrypted cache (ephemeral)

2. **Indexing:**
   - Decrypt entries in background
   - Build local search index (Core Data or FTS5)
   - Update index on new entries

3. **Search:**
   - Query local index
   - NSPredicate for Core Data
   - Or FTS5 queries for SQLite
   - Rank results locally

**Core Data Example (Reference):**

Agent implements search with:
- NSPredicate for content matching
- NSSortDescriptor for ranking
- Batch fetching for performance

**Performance:**
- Use background context for indexing
- Lazy loading of results
- Limit results (pagination)

---

#### Android Implementation

**Technology:**
- Room with FTS4/FTS5
- Or SQLite with FTS4/FTS5
- Or in-memory search with libraries

**Approach:**

1. **Room FTS Entity:**
   - Create FTS4/FTS5 table for searchable content
   - Populate on entry creation/update
   - Query with FTS queries

2. **Search:**
   - Query FTS table
   - MATCH operator for full-text search
   - RANK for relevance

**Room FTS Example (Reference):**

Agent creates FTS entity for entries with:
- Content field
- Tags field
- FTS4 or FTS5 table
- DAO with search methods

**Performance:**
- Background indexing (Coroutines)
- Limit results
- Debounce search input (avoid excessive queries)

---

#### Web Implementation

**Technology:**
- IndexedDB with JavaScript search
- Or libraries: Fuse.js, FlexSearch, Lunr.js
- Or Web Workers for background search

**Approach:**

1. **IndexedDB Storage:**
   - Store encrypted entries
   - Decrypt on-demand for search

2. **Search Libraries:**
   - **Fuse.js:** Fuzzy search, good for small-medium datasets
   - **FlexSearch:** Fast, good for large datasets
   - **Lunr.js:** Full-text search, similar to Elasticsearch

3. **Web Workers:**
   - Offload search to background thread
   - Avoid blocking UI
   - Good for large datasets

**Agent should:**
- Choose library based on dataset size
- Implement search in Web Worker
- Cache search index in memory
- Update index on sync

**Example with Fuse.js (Reference):**

```javascript
// Reference only - agent implements
import Fuse from 'fuse.js';

const options = {
  keys: ['decryptedContent', 'tags'],
  threshold: 0.3,
  includeScore: true
};

const fuse = new Fuse(decryptedEntries, options);
const results = fuse.search(query);
```

---

### E2E Search UI Requirements

**Agent must implement:**

1. **Information Banner:**
   - "Client-side search only - searches your local device"
   - "For full cross-device search, use UCE tier"

2. **Search Limitations:**
   - Show "Searching X entries on this device"
   - "Some entries may not be available if not synced"

3. **Offline Indicator:**
   - Show when offline (benefit)
   - "Search works offline"

4. **Performance Warning:**
   - If > 10,000 entries: "Search may be slow with large dataset"

---

## 🎨 Search UI Specifications (All Platforms)

### Search Input Component

**Requirements:**
- Search bar at top
- Clear button (X icon)
- Search icon
- Placeholder text based on tier:
  - UCE: "Search all your entries..."
  - E2E: "Search local entries..."

**Behavior:**
- Debounce input (300-500ms)
- Show loading indicator during search
- Clear results on empty input

---

### Search Suggestions (UCE Only)

**Requirements:**
- Show suggestions as user types (after 2+ characters)
- Suggest:
  - Recent searches (from user's history)
  - Popular tags
  - Partial matches
- Tap suggestion to search

**Implementation:**
- Store recent searches locally
- API endpoint for suggestions (optional)
- Limit to 5-10 suggestions

---

### Search Results Display

**Requirements:**
- Same entry card format as timeline
- Highlight search terms in preview (optional)
- Show relevance indicator (optional)
- Group by date (optional)
- Infinite scroll or pagination

**Empty State:**
- "No results found for 'query'"
- Suggestions:
  - Check spelling
  - Try different keywords
  - Clear filters

---

### Search Filters

**Filter Panel/Sheet:**

**Available Filters:**
1. **Tags:** Multi-select checkbox list
2. **Date Range:** Date picker (start/end)
3. **Source:** Radio buttons (diary, facebook, instagram, all)
4. **Sort:** Dropdown (relevance, date-newest, date-oldest)

**Filter Chips:**
- Show active filters as chips
- Tap chip to remove filter
- "Clear all" button

**Apply/Reset:**
- "Apply Filters" button
- "Reset" button

---

## 📊 Search Analytics (Optional)

**If implemented, track:**
- Search queries (anonymized)
- Search result clicks
- Zero-result queries (for improvement)
- Filter usage

**Privacy:**
- Never log actual search terms for E2E users
- Anonymize all analytics
- Aggregate only
- Respect user privacy settings

---

## 🔐 Security Considerations

### UCE Search Security

**Agent must ensure:**

1. **User Isolation:**
   - Search queries scoped to user_id
   - Prevent SQL injection (parameterized queries)
   - Validate all inputs

2. **Encryption:**
   - Search index encrypted at rest
   - Decryption only during indexing
   - Never store plaintext in logs

3. **Access Control:**
   - Verify JWT before search
   - Rate limiting (prevent abuse)
   - Monitor for unusual patterns

---

### E2E Search Security

**Agent must ensure:**

1. **No Server Queries:**
   - All search happens locally
   - No search terms sent to server
   - No search analytics sent to server

2. **Local Encryption:**
   - Decrypted cache cleared on app close
   - Secure local storage
   - No plaintext in logs

---

## ⚡ Performance Requirements

### UCE Server-Side Search

**Targets:**
- Query time: < 200ms (95th percentile)
- Index update: < 500ms per entry
- Support: 1000 concurrent searches
- Scalability: Up to 100,000 entries per user

**Monitoring:**
- Track query times
- Monitor index size
- Alert on slow queries
- Capacity planning

---

### E2E Client-Side Search

**Targets:**
- Query time: < 500ms for 10,000 entries
- Index build: Background, non-blocking
- Memory usage: < 50MB for index
- Battery impact: Minimal

**Optimization:**
- Incremental indexing
- Lazy loading results
- Efficient algorithms
- Background processing

---

## 🧪 Testing Requirements

### UCE Search Testing

**Agent must test:**

1. **Functional Tests:**
   - Keyword search
   - Phrase search
   - Multiple keywords
   - Filter combinations
   - Pagination
   - Empty results

2. **Performance Tests:**
   - Query time with 1K, 10K, 100K entries
   - Concurrent searches
   - Index update time
   - Memory usage

3. **Security Tests:**
   - SQL injection attempts
   - User isolation
   - Rate limiting
   - Access control

---

### E2E Search Testing

**Agent must test:**

1. **Functional Tests:**
   - Keyword search
   - Tag filtering
   - Date filtering
   - Offline search
   - Empty results

2. **Performance Tests:**
   - Search time with 1K, 5K, 10K entries
   - Memory usage
   - Battery impact
   - Index build time

3. **User Experience Tests:**
   - Search while syncing
   - Search after fresh install
   - Search with network offline

---

## 📈 Future Enhancements

**Post-MVP Features to Consider:**

1. **Advanced Search:**
   - Boolean operators (AND, OR, NOT)
   - Wildcard search (vac*)
   - Regular expressions
   - Search within date range

2. **AI-Powered Search (UCE Only):**
   - Semantic search (meaning-based)
   - Natural language queries
   - Auto-categorization
   - Related entries

3. **Search History:**
   - Save searches
   - Quick access to frequent searches
   - Search templates

4. **Saved Searches/Smart Folders:**
   - Save filter combinations
   - Auto-update with new matches
   - Notifications for new matches

---

## 📝 Implementation Checklist

### For UCE Tier

**Backend:**
- [ ] Add search_vector column to entries table
- [ ] Create GIN index
- [ ] Implement indexing on entry create/update
- [ ] Create search service abstraction
- [ ] Implement PostgreSQL FTS search
- [ ] Add relevance ranking
- [ ] Create search API endpoint
- [ ] Add pagination
- [ ] Implement filters
- [ ] Add rate limiting
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Performance testing

**Frontend:**
- [ ] Create search UI component
- [ ] Implement search input with debounce
- [ ] Create filter panel
- [ ] Implement search suggestions
- [ ] Display search results
- [ ] Handle empty states
- [ ] Add loading states
- [ ] Test with large datasets

---

### For E2E Tier

**iOS:**
- [ ] Set up Core Data FTS or SQLite FTS5
- [ ] Implement local indexing
- [ ] Create search service
- [ ] Build search UI
- [ ] Add filters
- [ ] Implement info banner
- [ ] Test performance with large datasets
- [ ] Optimize battery usage

**Android:**
- [ ] Set up Room FTS4/FTS5
- [ ] Implement local indexing
- [ ] Create search repository
- [ ] Build search UI
- [ ] Add filters
- [ ] Implement info banner
- [ ] Test performance
- [ ] Optimize battery usage

**Web:**
- [ ] Choose search library (Fuse.js/FlexSearch)
- [ ] Implement IndexedDB search
- [ ] Create Web Worker for search
- [ ] Build search UI
- [ ] Add filters
- [ ] Implement info banner
- [ ] Test performance
- [ ] Optimize memory usage

---

## 🎯 Success Criteria

**UCE Search:**
- [ ] Full-text search works correctly
- [ ] Response time < 200ms (p95)
- [ ] Relevance ranking accurate
- [ ] Filters work correctly
- [ ] Handles 100K+ entries per user
- [ ] Zero SQL injection vulnerabilities

**E2E Search:**
- [ ] Local search works offline
- [ ] Search time < 500ms for 10K entries
- [ ] No search queries sent to server
- [ ] Filters work correctly
- [ ] Info banner clearly explains limitations
- [ ] Memory and battery efficient

---

**End of Search Implementation Specification**

Agent should use this specification to implement search that respects the encryption model while providing the best possible user experience for each tier.
