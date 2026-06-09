FROM node:22

RUN corepack enable pnpm

WORKDIR /app

COPY package.json ./
RUN pnpm install

COPY . .

EXPOSE 5173

CMD ["pnpm", "dev", "--", "--host", "0.0.0.0"]
