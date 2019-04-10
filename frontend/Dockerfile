# Stage 0
FROM node:8.15
USER node
ENV NPM_CONFIG_PREFIX=/home/node/.npm-global
ENV PATH=$PATH:/home/node/.npm-global/bin
RUN npm install -g create-elm-app@3.0.6 --unsafe-perm=true
WORKDIR /app
COPY elm.json /app/elm.json
COPY src /app/src/
COPY public /app/public/
USER root
RUN elm-app build

# Stage 1, based on Nginx
FROM nginx:1.15
COPY --from=0 /app/build/ /usr/share/nginx/html
COPY default.conf /etc/nginx/conf.d/default.conf
