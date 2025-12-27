## 🚀 CI/CD Pipeline

### GitHub Actions
- **CI Pipeline**: Chạy tests trên mỗi push/pull request
- **CD Pipeline**: Tự động deploy lên server khi push lên main branch

### Local Development
```bash
# Chạy với Docker
docker-compose up -d

# Build và chạy tests
mvn clean test

# Package
mvn clean package
