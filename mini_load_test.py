import requests
import json
import time
import argparse

# Configuration
BASE_URL = "http://localhost:8080"
USER_ID = "testUserPrometheus"
SLEEP_BETWEEN_REQUESTS = 0.1  # Seconds, for pacing

def generate_token():
    url = f"{BASE_URL}/api/token/generate"
    payload = {"userId": USER_ID}
    response = requests.post(url, json=payload)
    print(f"Generate Response: {response.text}")
    if response.status_code == 200:
        try:
            return response.json().get("token")
        except json.JSONDecodeError:
            print("Invalid JSON response.")
    return None

def validate_token(token):
    url = f"{BASE_URL}/api/token/validate"
    payload = {"token": token}
    response = requests.post(url, json=payload)
    print(f"Validating Token '{token[:10]}...': {response.status_code} - {response.text}")

def mini_load_test(num_requests):
    for i in range(num_requests):
        print(f"\n=== Iteration {i + 1} ===")
        token = generate_token()
        if token:
            validate_token(token)
            validate_token("anInvalidTokenOnPurpose")
        else:
            print("Failed to generate a valid token.")
        time.sleep(SLEEP_BETWEEN_REQUESTS)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Mini load tester for token service.")
    parser.add_argument("num_requests", type=int, help="Number of request iterations to perform")
    args = parser.parse_args()

    mini_load_test(args.num_requests)
