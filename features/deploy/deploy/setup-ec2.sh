#!/usr/bin/env bash
set -euo pipefail

sudo apt-get update
sudo apt-get install -y openjdk-21-jre-headless ca-certificates curl gnupg nginx

sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

id -u core &>/dev/null || sudo useradd --system --create-home --home-dir /opt/core-app --shell /usr/sbin/nologin core
sudo usermod -aG docker core

sudo mkdir -p /opt/core-app
sudo chown core:core /opt/core-app

sudo mkdir -p /var/www/core-front
sudo chown -R www-data:www-data /var/www/core-front

if [ -f "$(dirname "$0")/nginx-core-app.conf" ]; then
  sudo cp "$(dirname "$0")/nginx-core-app.conf" /etc/nginx/sites-available/core-app
  sudo ln -sf /etc/nginx/sites-available/core-app /etc/nginx/sites-enabled/core-app
  sudo rm -f /etc/nginx/sites-enabled/default
  sudo nginx -t
  sudo systemctl reload nginx
fi

echo "Bootstrap concluido. Proximo passo: copiar docker-compose.yml e criar /opt/core-app/.env (ver DEPLOY.md)."
