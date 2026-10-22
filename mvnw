#!/bin/sh
# Run the auth-service Maven Wrapper from the repository root.
set -eu
cd "$(dirname "$0")/services/auth-service"
exec sh ./mvnw "$@"
