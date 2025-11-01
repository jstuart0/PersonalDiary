# Backend Deployment Guide for Kubernetes Cluster 'thor'

## Prerequisites

1. **Kubernetes cluster 'thor' access**:
   ```bash
   kubectl config use-context thor
   kubectl cluster-info
   ```

2. **Database and Redis deployed**:
   - PostgreSQL must be running: `postgres-service.personal-diary.svc.cluster.local:5432`
   - Redis must be running: `redis-service.personal-diary.svc.cluster.local:6379`

3. **Container image built**:
   ```bash
   cd /Users/jaystuart/dev/personal-diary/backend
   docker build -t personal-diary/backend:latest .
   ```

4. **Secrets configured**:
   - Edit `kubernetes/backend/secret.yaml` and replace placeholder values
   - Generate SECRET_KEY: `python -c "import secrets; print(secrets.token_urlsafe(64))"`
   - Configure AWS S3 credentials
   - Configure Facebook OAuth credentials

## Deployment Steps

### 1. Verify Context
```bash
kubectl config current-context  # Should show: thor
```

### 2. Create/Update Secrets
```bash
# IMPORTANT: Edit secret.yaml with real values first!
kubectl apply -f kubernetes/backend/secret.yaml
```

### 3. Deploy ConfigMap
```bash
kubectl apply -f kubernetes/backend/configmap.yaml
```

### 4. Deploy Backend API
```bash
kubectl apply -f kubernetes/backend/deployment.yaml
kubectl apply -f kubernetes/backend/service.yaml
```

### 5. Deploy Celery Workers
```bash
kubectl apply -f kubernetes/backend/celery-worker.yaml
```

### 6. Deploy Ingress (with TLS)
```bash
kubectl apply -f kubernetes/backend/ingress.yaml
```

### 7. Deploy Horizontal Pod Autoscaler (Optional)
```bash
kubectl apply -f kubernetes/backend/hpa.yaml
```

### 8. Verify Deployment
```bash
# Check pods
kubectl -n personal-diary get pods -l app.kubernetes.io/component=backend

# Check logs
kubectl -n personal-diary logs -l app.kubernetes.io/component=backend --tail=50

# Check service
kubectl -n personal-diary get svc backend-service

# Check ingress
kubectl -n personal-diary get ingress backend-ingress

# Test health endpoint
kubectl -n personal-diary port-forward svc/backend-service 8000:8000
curl http://localhost:8000/health
```

## DNS Configuration

Add DNS record for the backend API:
- **Domain**: `api.diary.xmojo.net`
- **Type**: A
- **Value**: 192.168.60.50 (Traefik Load Balancer IP)

## Database Migrations

Run database migrations after first deployment:
```bash
# Get a backend pod name
POD=$(kubectl -n personal-diary get pods -l app.kubernetes.io/component=backend -o jsonpath='{.items[0].metadata.name}')

# Run migrations
kubectl -n personal-diary exec -it $POD -- alembic upgrade head
```

## Monitoring

### View Logs
```bash
# Backend API logs
kubectl -n personal-diary logs -f -l app.kubernetes.io/component=backend

# Celery worker logs
kubectl -n personal-diary logs -f -l app.kubernetes.io/component=celery-worker

# Celery beat logs
kubectl -n personal-diary logs -f -l app.kubernetes.io/component=celery-beat
```

### Check Resource Usage
```bash
kubectl -n personal-diary top pods -l app.kubernetes.io/name=personal-diary
```

### Check HPA Status
```bash
kubectl -n personal-diary get hpa backend-hpa
```

## Scaling

### Manual Scaling
```bash
# Scale backend replicas
kubectl -n personal-diary scale deployment backend --replicas=5

# Scale Celery workers
kubectl -n personal-diary scale deployment celery-worker --replicas=4
```

### Auto-scaling
The HPA will automatically scale between 3-10 replicas based on CPU/memory usage.

## Rollback

```bash
# View deployment history
kubectl -n personal-diary rollout history deployment backend

# Rollback to previous version
kubectl -n personal-diary rollout undo deployment backend

# Rollback to specific revision
kubectl -n personal-diary rollout undo deployment backend --to-revision=2
```

## Troubleshooting

### Pods not starting
```bash
# Check pod status
kubectl -n personal-diary describe pod <pod-name>

# Check events
kubectl -n personal-diary get events --sort-by='.lastTimestamp'
```

### Database connection issues
```bash
# Test PostgreSQL connectivity
kubectl -n personal-diary run -it --rm debug --image=postgres:16 --restart=Never -- \
  psql -h postgres-service.personal-diary.svc.cluster.local -U diary_user -d personal_diary

# Test Redis connectivity
kubectl -n personal-diary run -it --rm debug --image=redis:7 --restart=Never -- \
  redis-cli -h redis-service.personal-diary.svc.cluster.local ping
```

### Certificate issues
```bash
# Check certificate status
kubectl -n personal-diary get certificate backend-tls

# Check cert-manager logs
kubectl -n cert-manager logs -l app=cert-manager
```

## Update Deployment

```bash
# Update image
kubectl -n personal-diary set image deployment/backend backend=personal-diary/backend:v2.0

# Or apply updated manifests
kubectl apply -f kubernetes/backend/deployment.yaml

# Watch rollout
kubectl -n personal-diary rollout status deployment backend
```

## Clean Up

```bash
# Remove all backend resources
kubectl delete -f kubernetes/backend/

# Or remove entire namespace
kubectl delete namespace personal-diary
```

## API Testing

Once deployed, test the API:

```bash
# Health check
curl https://api.diary.xmojo.net/health

# View API docs
open https://api.diary.xmojo.net/api/v1/docs

# Test signup
curl -X POST https://api.diary.xmojo.net/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"TestPassword123!","encryption_tier":"uce"}'
```

## Performance Optimization

### Resource Limits
Adjust based on actual usage:
- Increase memory limits if OOM errors occur
- Increase CPU limits if experiencing slowness
- Monitor with: `kubectl -n personal-diary top pods`

### Connection Pooling
Database pool size is configured in ConfigMap:
- `DATABASE_POOL_SIZE: "10"`
- `DATABASE_MAX_OVERFLOW: "20"`

Adjust based on number of replicas and traffic.

## Security Checklist

- [ ] Secrets properly configured (not using placeholder values)
- [ ] TLS certificate issued by Let's Encrypt
- [ ] Database credentials rotated from defaults
- [ ] Network policies applied (if needed)
- [ ] Pod security policies configured
- [ ] Resource limits set appropriately
- [ ] CORS origins restricted to production domains
