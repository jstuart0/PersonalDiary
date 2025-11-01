# Command Reference

## Kubernetes Commands

### Context and Cluster Info

```bash
# List all contexts
kubectl config get-contexts

# Get current context
kubectl config current-context

# Switch context
kubectl config use-context thor

# Cluster info
kubectl cluster-info
kubectl version
```

### Node and Resource Monitoring

```bash
# List nodes
kubectl get nodes -o wide

# Node resource usage
kubectl top nodes

# Describe node
kubectl describe node <node-name>
```

### Namespace Operations

```bash
# List namespaces
kubectl get namespaces

# Create namespace
kubectl create namespace <namespace>

# Delete namespace
kubectl delete namespace <namespace>
```

### Pod Operations

```bash
# List all pods
kubectl get pods -A

# List pods in namespace
kubectl get pods -n <namespace>

# Describe pod
kubectl describe pod <pod-name> -n <namespace>

# View logs
kubectl logs -f <pod-name> -n <namespace>
kubectl logs --previous <pod-name> -n <namespace>  # Previous container logs

# Execute command in pod
kubectl exec -it <pod-name> -n <namespace> -- /bin/bash

# Watch pods
kubectl get pods -n <namespace> -w
```

### Service Operations

```bash
# List all services
kubectl get svc -A

# Describe service
kubectl describe svc <service-name> -n <namespace>

# Port forward
kubectl port-forward -n <namespace> svc/<service-name> <local-port>:<service-port>
```

### Deployment Operations

```bash
# List deployments
kubectl get deployments -A

# Describe deployment
kubectl describe deployment <deployment-name> -n <namespace>

# Rollout status
kubectl rollout status deployment/<deployment-name> -n <namespace>

# Rollout history
kubectl rollout history deployment/<deployment-name> -n <namespace>

# Rollback deployment
kubectl rollout undo deployment/<deployment-name> -n <namespace>

# Restart deployment
kubectl rollout restart deployment/<deployment-name> -n <namespace>

# Scale deployment
kubectl scale deployment/<deployment-name> --replicas=<count> -n <namespace>
```

### ConfigMap and Secret Operations

```bash
# List ConfigMaps
kubectl get configmaps -A

# List secrets
kubectl get secrets -A

# Describe secret
kubectl describe secret <secret-name> -n <namespace>

# Get secret value (base64 decoded)
kubectl get secret <secret-name> -n <namespace> -o jsonpath='{.data.<key>}' | base64 -d
```

### Ingress Operations

```bash
# List ingresses
kubectl get ingress -A

# Describe ingress
kubectl describe ingress <ingress-name> -n <namespace>
```

### PersistentVolume and PVC Operations

```bash
# List PVs
kubectl get pv

# List PVCs
kubectl get pvc -A

# Describe PV
kubectl describe pv <pv-name>

# Describe PVC
kubectl describe pvc <pvc-name> -n <namespace>
```

### Events and Troubleshooting

```bash
# Get events (sorted by time)
kubectl get events -A --sort-by='.lastTimestamp'
kubectl get events -n <namespace> --sort-by='.lastTimestamp'

# Get all resources in namespace
kubectl get all -n <namespace>

# Apply manifest
kubectl apply -f <manifest-file>

# Delete resources
kubectl delete -f <manifest-file>
```

## Docker Commands

### Image Operations

```bash
# List images
docker images

# Build image
docker build -t <image-name>:<tag> .
docker build --no-cache -t <image-name>:<tag> .
docker build --platform linux/amd64 -t <image-name>:<tag> .

# Tag image
docker tag <source-image> <target-image>

# Push image
docker push <image-name>:<tag>

# Pull image
docker pull <image-name>:<tag>

# Remove image
docker rmi <image-name>:<tag>
```

### Container Operations

```bash
# List containers
docker ps
docker ps -a

# View logs
docker logs -f <container-name>

# Execute command in container
docker exec -it <container-name> /bin/bash

# Stop container
docker stop <container-name>

# Start container
docker start <container-name>

# Remove container
docker rm <container-name>
```

### Docker Compose

```bash
# Start services
docker compose up -d

# Stop services
docker compose down

# View logs
docker compose logs -f

# Restart service
docker compose restart <service-name>

# Build services
docker compose build
docker compose build --no-cache
```

## Network Commands

### UniFi UDM Pro

```bash
# SSH to UDM Pro
ssh admin@192.168.10.1

# View active connections (if SSH enabled)
ubnt-device-info
```

## Storage Commands

### Synology

```bash
# SSH to Synology (if enabled)
ssh admin@192.168.10.159

# Check volume status
df -h

# Check RAID status
cat /proc/mdstat
```

### Ceph

```bash
# SSH to Ceph node
ssh root@192.168.10.11  # Or Node 2/3

# Cluster health
ceph -s
ceph health detail
ceph df

# OSD status
ceph osd status
ceph osd tree
ceph osd df

# Pool operations
ceph osd pool ls detail
ceph osd pool stats
```

## Useful One-Liners

### Find and Kill Process by Port

```bash
# Find process on port
lsof -i :<port>

# Kill process on port (macOS/Linux)
kill -9 $(lsof -t -i:<port>)
```

### Watch kubectl get pods

```bash
# Auto-refresh every 2 seconds
watch -n 2 kubectl get pods -n <namespace>
```

### Get all images in pods

```bash
kubectl get pods -A -o jsonpath='{range .items[*]}{.spec.containers[*].image}{"\n"}{end}' | sort | uniq
```

### Delete all pods in namespace

```bash
kubectl delete pods --all -n <namespace>
```

### Get pod IPs

```bash
kubectl get pods -n <namespace> -o wide
```

### Tail logs from multiple pods

```bash
kubectl logs -f -l app=<label> -n <namespace>
```
