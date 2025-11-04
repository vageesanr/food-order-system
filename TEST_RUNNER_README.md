# Test Runner Utility

A utility script to manage test runs, automatically save test data, and rerun failed tests with the same parameters.

## Features

- **Automatic Test Saving**: Every test run saves test data (test ID, orders, parameters) to a JSON file
- **Rerun Failed Tests**: Rerun any saved test with the exact same parameters
- **Continuous Testing**: Run tests continuously until a failure
- **Test Management**: List and manage saved tests

## Prerequisites

- Docker (with the `fulfillment-system` image built)
- Python 3.6+

## Usage

### Run a New Test

Run a new test and automatically save it:

```bash
./run_tests.py <auth_token> new
```

With custom parameters:

```bash
./run_tests.py <auth_token> new --rate-ms 500 --min-pickup-ms 4000 --max-pickup-ms 8000 --seed 12345
```

### List Saved Tests

List all saved tests (shows only failed/unfinished by default):

```bash
./run_tests.py <auth_token> list
```

Show all tests:

```bash
./run_tests.py <auth_token> list --all
```

### Rerun a Failed Test

Rerun a specific saved test:

```bash
./run_tests.py <auth_token> rerun tests/test_20231104_193045.json
```

Or rerun by test number (from the list):

```bash
# First, list tests to find the test number
./run_tests.py <auth_token> list

# Then rerun (example with test file name)
./run_tests.py <auth_token> rerun tests/test_20231104_193045.json
```

### Run Tests Continuously

Run tests continuously until a failure or max tests reached:

```bash
# Run until failure
./run_tests.py <auth_token> continuous

# Run up to 10 tests
./run_tests.py <auth_token> continuous --max-tests 10
```

## Test Data Format

Each test is saved as a JSON file in the `tests/` directory with the following structure:

```json
{
  "testId": "22",
  "orders": [
    {
      "id": "abc123",
      "name": "Cheese Pizza",
      "temp": "hot",
      "price": 10.0,
      "freshness": 120
    }
  ],
  "rateMicros": 500000,
  "minPickupMicros": 4000000,
  "maxPickupMicros": 8000000,
  "seed": null,
  "result": "pass",
  "timestamp": "20231104_193045"
}
```

## Example Workflow

1. **Run tests continuously** until you hit a failure:
   ```bash
   ./run_tests.py <auth_token> continuous
   ```

2. **When a test fails**, it will be automatically saved. List failed tests:
   ```bash
   ./run_tests.py <auth_token> list
   ```

3. **Fix your code** and rebuild the Docker image:
   ```bash
   docker build -t fulfillment-system .
   ```

4. **Rerun the failed test** to verify the fix:
   ```bash
   ./run_tests.py <auth_token> rerun tests/test_20231104_193045.json
   ```

5. **If it passes**, continue with continuous testing:
   ```bash
   ./run_tests.py <auth_token> continuous
   ```

## Command Line Options

### Global Options

- `--docker-image`: Docker image name (default: `fulfillment-system`)
- `--test-dir`: Directory to store test files (default: `tests`)

### New Test Options

- `--rate-ms`: Order placement rate in milliseconds (default: 500)
- `--min-pickup-ms`: Minimum pickup time in milliseconds (default: 4000)
- `--max-pickup-ms`: Maximum pickup time in milliseconds (default: 8000)
- `--seed`: Seed for reproducible test problems

### Continuous Test Options

- `--max-tests`: Maximum number of tests to run (default: unlimited)
- `--rate-ms`, `--min-pickup-ms`, `--max-pickup-ms`: Same as new test options

## Direct Docker Usage

You can also use the save/load features directly with Docker:

### Save Test Data

```bash
docker run --rm -v $(pwd)/tests:/tests fulfillment-system \
  <auth_token> 500 4000 8000 \
  --save-test /tests/test_$(date +%Y%m%d_%H%M%S).json
```

### Load Test Data

```bash
docker run --rm -v $(pwd)/tests:/tests fulfillment-system \
  --load-test /tests/test_20231104_193045.json <auth_token>
```

## Notes

- Test files are saved in the `tests/` directory by default
- Each test file includes the test ID, orders, parameters, and result
- Rerunning a test will update the result in the test file
- The test runner uses Docker volume mounts to access test files
