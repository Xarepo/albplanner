FROM gradle:8.0.2-jdk19-alpine AS build

WORKDIR /app

EXPOSE 3001

COPY . /app
