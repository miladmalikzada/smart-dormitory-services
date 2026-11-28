#!/usr/bin/env python3
"""Creates the private .env file with random passwords and a random AES-256 key.

    python3 scripts/setup_env.py           # create .env (does nothing if it exists)
    python3 scripts/setup_env.py --force   # replace it (old encrypted data becomes unreadable!)
"""
import base64, os, secrets, sys
from pathlib import Path

path = Path(__file__).resolve().parent.parent / ".env"
if path.exists() and "--force" not in sys.argv:
    print(f"{path} already exists - left unchanged (use --force to replace it).")
    sys.exit(0)

def password():
    return secrets.token_urlsafe(12)

lines = [
    "# Private settings for Smart Dormitory Services. Do NOT commit this file.",
    f"DORM_ADMIN_PASSWORD={password()}",
    f"DORM_ENCRYPTION_KEY={base64.b64encode(secrets.token_bytes(32)).decode()}",
    "",
]
path.write_text("\n".join(lines))
os.chmod(path, 0o600)
print(f"Created {path}")
print("Open it to see the administrator password (user name: admin).")
print("Everyone else creates their own account on the sign-up page.")
