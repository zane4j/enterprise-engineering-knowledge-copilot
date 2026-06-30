# Production Deployment

## Deployment model

GitHub Actions verifies the repository, builds immutable API and worker images, publishes them to GHCR, and invokes a restricted SSH deployment script. The server pulls images and runs Docker Compose; it does not compile Maven projects.

## One-time server bootstrap

1. Install Docker Engine, Docker Compose plugin, and Git.
2. Create a non-root `deploy` user in the `docker` group.
3. Clone this repository into `/opt/enterprise-engineering-knowledge-copilot` as `deploy`.
4. Copy `deploy/production.env.example` to `config/production.env`; set a real domain, strong database/MinIO passwords, and production model credentials.
5. Log in on the server to GHCR using a fine-grained token with `read:packages` only.
6. Add the GitHub Actions deployment public key to `/home/deploy/.ssh/authorized_keys`.
7. Create the GitHub `production` Environment and configure `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`, and `DEPLOY_KNOWN_HOSTS`.
8. Point the domain A/AAAA records to the server and allow inbound TCP 80/443. Do not publicly expose 5432, 6379, 9000, or 9001.

## First deployment

Set `APP_IMAGE_TAG` in `config/production.env` to a published commit SHA, then run:

```bash
/opt/enterprise-engineering-knowledge-copilot/deploy/deploy.sh
```

Caddy obtains and renews HTTPS certificates automatically once DNS resolves to the server and ports 80/443 are reachable.

## Rollback

Edit only `APP_IMAGE_TAG` in `config/production.env` to a previously published commit SHA, then rerun `deploy.sh`.

## Security boundary

`production.env`, SSH private keys, GitHub tokens, and OpenAI keys must never be committed or pasted into chat. The current application still needs JWT-backed identity before public multi-user use; restrict access at the network or reverse-proxy layer until that is implemented.
