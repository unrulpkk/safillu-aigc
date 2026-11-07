FROM nginx:1.28-alpine
COPY lumina-studio/dist/ /usr/share/nginx/html/
COPY zaomeng-backend/deploy/nginx.conf /etc/nginx/conf.d/default.conf
