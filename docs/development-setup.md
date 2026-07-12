# OpenCrowd — Development Environment Setup

This guide walks through setting up the complete development environment for OpenCrowd on AWS EC2, connected via Kiro IDE (VS Code SSH Remote).

---

## Overview

| Component | Where It Runs |
|-----------|--------------|
| Kiro IDE (editor, UI) | Your Mac (lightweight) |
| Source code, builds, Docker | EC2 instance (via SSH Remote) |
| PostgreSQL, Redis, Keycloak | Docker Compose on EC2 |
| Frontend dev server | EC2 (port 3000) |
| Backend (Spring Boot) | EC2 (port 8080) |

```
Your Mac (Kiro IDE) ──SSH Remote──► EC2 t3.xlarge (16GB RAM)
                                      ├── Docker Compose
                                      │   ├── PostgreSQL 16
                                      │   ├── Valkey (Redis)
                                      │   └── Keycloak 24
                                      ├── JDK 21 (backend)
                                      ├── Node 20 + pnpm (frontend)
                                      └── Source code (git)
```

---

## Prerequisites (Your Mac)

You only need these installed locally:

| Tool | Purpose | Install |
|------|---------|---------|
| Kiro IDE | Editor + SSH Remote | Already installed |
| SSH client | Connect to EC2 | Built into macOS |
| Web browser | Access dev services | Already have |
| AWS Console access | Manage EC2 | Browser-based |

Everything else (Docker, JDK, Node, etc.) runs on EC2.

---

## Step 1: Launch EC2 Instance

### 1.1 Instance Configuration

Go to **AWS Console → EC2 → Launch Instance** and configure:

| Setting | Value |
|---------|-------|
| Name | `opencrowd-dev` |
| Region | Choose closest to your location |
| AMI | Ubuntu 24.04 LTS (x86_64 / amd64) |
| Instance type | `t3.xlarge` (4 vCPU, 16GB RAM) |
| Key pair | Create new → `opencrowd-dev` → Download `.pem` file |
| Root volume | 50 GB, gp3 |
| Security group | Create new → `opencrowd-dev-sg` |

### 1.2 Security Group Rules (Inbound)

| Type | Port | Source | Purpose |
|------|------|--------|---------|
| SSH | 22 | My IP (`x.x.x.x/32`) | SSH access |
| Custom TCP | 3000 | My IP | Frontend dev server |
| Custom TCP | 8080 | My IP | Backend API + Swagger |
| Custom TCP | 8180 | My IP | Keycloak admin console |

> To find your IP: search "what is my ip" in your browser. Use that IP with `/32` suffix.

> **Security note:** Never open these ports to `0.0.0.0/0`. Always restrict to your IP.

### 1.3 Allocate Elastic IP

1. Go to **EC2 → Elastic IPs → Allocate Elastic IP address**
2. Click **Allocate**
3. Select the new IP → **Actions → Associate Elastic IP address**
4. Choose your `opencrowd-dev` instance
5. Click **Associate**

This gives you a fixed IP that doesn't change when you stop/start the instance.

> **Cost note:** Elastic IPs are free while associated with a running instance. You're charged ~$3.65/month if the instance is stopped but the IP remains allocated.

---

## Step 2: Configure SSH on Your Mac

### 2.1 Save and Secure Key File

```bash
# Move key to SSH directory
mkdir -p ~/.ssh
mv ~/Downloads/opencrowd-dev.pem ~/.ssh/opencrowd-dev.pem

# Set correct permissions (required — SSH will reject insecure key files)
chmod 400 ~/.ssh/opencrowd-dev.pem
```

### 2.2 Add SSH Config Entry

Edit `~/.ssh/config` (create if it doesn't exist):

```bash
nano ~/.ssh/config
```

Add this block:

```
Host opencrowd-dev
    HostName <YOUR-ELASTIC-IP>
    User ubuntu
    IdentityFile ~/.ssh/opencrowd-dev.pem
    ForwardAgent yes
    ServerAliveInterval 60
    ServerAliveCountMax 3
```

Replace `<YOUR-ELASTIC-IP>` with your actual Elastic IP address.

### 2.3 Test SSH Connection

```bash
ssh opencrowd-dev
```

You should see the Ubuntu welcome message. If it works, proceed to Step 3.

---

## Step 3: Install Development Tools on EC2

SSH into the instance and run the following setup script.

### 3.1 System Update

```bash
sudo apt update && sudo apt upgrade -y
```

### 3.2 Install Docker

```bash
# Add Docker's official GPG key
sudo apt install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

# Add Docker repository
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker Engine + Compose
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Allow running Docker without sudo
sudo usermod -aG docker $USER
```

### 3.3 Install JDK 21

```bash
sudo apt install -y openjdk-21-jdk
```

### 3.4 Install Node.js 20 LTS

```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
```

### 3.5 Install pnpm

```bash
npm install -g pnpm
```

### 3.6 Install Kotlin & Gradle (via SDKMAN)

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install kotlin
sdk install gradle
```

### 3.7 Install Utilities

```bash
sudo apt install -y git htop jq unzip make
```

### 3.8 Apply Group Changes

**Log out and back in** for the Docker group membership to take effect:

```bash
exit
ssh opencrowd-dev
```

---

## Step 4: Verify Installation

Run each command and confirm the expected output:

```bash
docker run hello-world          # Should print "Hello from Docker!"
docker compose version          # Should show v2.x

java --version                  # Should show openjdk 21.x
javac --version                 # Should show javac 21.x

node --version                  # Should show v20.x
pnpm --version                  # Should show 9.x or later

gradle --version                # Should show Gradle 8.x
kotlin -version                 # Should show kotlinc-jvm 1.9+

git --version                   # Should show git 2.x
```

If any command fails, re-run the relevant installation step.

---

## Step 5: Configure Git & GitHub

### 5.1 Set Git Identity

```bash
git config --global user.name "Your Name"
git config --global user.email "your@email.com"
```

### 5.2 Generate SSH Key for GitHub

```bash
ssh-keygen -t ed25519 -C "your@email.com"
# Press Enter for default file location
# Press Enter for no passphrase (or set one for extra security)
```

### 5.3 Add Key to GitHub

Display your public key:

```bash
cat ~/.ssh/id_ed25519.pub
```

Copy the entire output, then:

1. Go to **github.com → Settings → SSH and GPG keys**
2. Click **New SSH key**
3. Title: `opencrowd-dev-ec2`
4. Paste the key
5. Click **Add SSH key**

### 5.4 Test GitHub Connection

```bash
ssh -T git@github.com
# Should print: "Hi <username>! You've successfully authenticated..."
```

### 5.5 Create the Repository

On GitHub:
1. Go to **github.com → New repository** (or create an organization first: `opencrowd`)
2. Repository name: `opencrowd`
3. Visibility: Public (open source) or Private (switch to public later)
4. **Do NOT** initialize with README, .gitignore, or license (we'll push from EC2)

### 5.6 Initialize Project Directory

```bash
mkdir -p ~/projects
cd ~/projects
mkdir opencrowd
cd opencrowd
git init
git remote add origin git@github.com:<your-username-or-org>/opencrowd.git
```

---

## Step 6: Connect Kiro IDE via SSH Remote

### 6.1 Open Remote Connection

1. Open Kiro on your Mac
2. Press `Cmd+Shift+P`
3. Type: **"Remote-SSH: Connect to Host"**
4. Select: `opencrowd-dev`
5. Wait for Kiro to install the remote server component (first time only, ~30 seconds)

### 6.2 Open Project Folder

Once connected:
1. Click **"Open Folder"**
2. Navigate to: `/home/ubuntu/projects/opencrowd`
3. Click **OK**

You're now editing files on EC2 as if they were local.

### 6.3 Verify Port Forwarding

Kiro automatically forwards ports from EC2 to your Mac. When services are running:

| EC2 Port | Access From Mac | Service |
|----------|----------------|---------|
| 3000 | `http://localhost:3000` | Frontend |
| 8080 | `http://localhost:8080` | Backend API |
| 8180 | `http://localhost:8180` | Keycloak |

If auto-forwarding doesn't work, you can manually forward:
- `Cmd+Shift+P` → "Forward a Port" → enter the port number

---

## Step 7: Cost Management

### Stop When Not Working

The EC2 instance costs ~$0.17/hour when running. Stop it when you're done for the day:

**From AWS Console:**
- EC2 → Instances → Select `opencrowd-dev` → Instance State → Stop

**From your Mac terminal:**
```bash
aws ec2 stop-instances --instance-ids <your-instance-id> --region <your-region>
```

**To start again:**
```bash
aws ec2 start-instances --instance-ids <your-instance-id> --region <your-region>
```

> The Elastic IP stays the same. Your SSH config doesn't need to change.

### Monthly Cost Estimate

| State | Cost |
|-------|------|
| Running 8 hours/day, 5 days/week | ~$25-30/month |
| Running 24/7 | ~$120/month |
| Stopped (EBS storage only) | ~$4/month |
| Elastic IP (while stopped) | ~$3.65/month |

### Set a Billing Alarm

1. Go to **AWS → Billing → Budgets**
2. Create a budget: $50/month threshold
3. Get email alerts at 80% and 100%

---

## Troubleshooting

### SSH Connection Refused

```
ssh: connect to host <ip> port 22: Connection refused
```

- Instance might be stopped → Start it from AWS Console
- Security group doesn't have your current IP → Update inbound rule
- Your IP changed (common with home internet) → Update security group

### Docker Permission Denied

```
permission denied while trying to connect to the Docker daemon socket
```

- Log out and back in: `exit` then `ssh opencrowd-dev`
- Or run: `newgrp docker`

### Kiro Remote Connection Slow

- Check EC2 instance isn't running out of memory: `htop`
- Check network: instance should be in a region close to you
- Restart the remote connection: `Cmd+Shift+P` → "Remote-SSH: Kill VS Code Server on Host"

### Port Forwarding Not Working

- Check the service is actually running on EC2: `curl localhost:3000`
- Manually forward: `Cmd+Shift+P` → "Forward a Port"
- Check security group allows the port from your IP

---

## Quick Reference

### Daily Workflow

```bash
# Start your day (from Mac)
ssh opencrowd-dev              # Verify instance is accessible
# Open Kiro → Remote-SSH → opencrowd-dev

# Start services (from Kiro terminal on EC2)
cd ~/projects/opencrowd/infrastructure/docker
docker compose up -d           # Start PostgreSQL, Redis, Keycloak

# Start backend (from Kiro terminal)
cd ~/projects/opencrowd/backend
./gradlew :api:bootRun         # Starts on port 8080

# Start frontend (from another Kiro terminal)
cd ~/projects/opencrowd/frontend
pnpm dev                       # Starts on port 3000

# End your day
docker compose down            # Stop services
# Stop EC2 from AWS Console (or leave running if you prefer)
```

### Useful Commands

```bash
# Check what's running
docker ps                      # Running containers
htop                           # CPU/memory usage

# View logs
docker compose logs -f         # Follow all container logs
docker compose logs keycloak   # Specific service logs

# Reset database (start fresh)
docker compose down -v         # Remove volumes (deletes all data)
docker compose up -d           # Recreate with fresh data

# Rebuild after Docker changes
docker compose build --no-cache
docker compose up -d
```

---

## Checklist

Use this to confirm everything is ready:

- [ ] EC2 instance launched (t3.xlarge, Ubuntu 24.04, 50GB)
- [ ] Elastic IP allocated and associated
- [ ] Security group: SSH + 3000 + 8080 + 8180 (your IP only)
- [ ] SSH works: `ssh opencrowd-dev` connects successfully
- [ ] Docker installed and works without sudo
- [ ] JDK 21 installed: `java --version` shows 21
- [ ] Node 20 installed: `node --version` shows 20.x
- [ ] pnpm installed: `pnpm --version` works
- [ ] Gradle installed: `gradle --version` shows 8.x
- [ ] Kotlin installed: `kotlin -version` shows 1.9+
- [ ] Git configured with name and email
- [ ] SSH key added to GitHub
- [ ] `ssh -T git@github.com` authenticates successfully
- [ ] GitHub repo created
- [ ] Project directory initialized with git remote
- [ ] Kiro connects via Remote-SSH and opens project folder
- [ ] Billing alarm set

**Once all boxes are checked, the development environment is ready. Ping Kiro and we start building.**
