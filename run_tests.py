#!/usr/bin/env python3
"""
Test runner utility for Cloud Kitchen Fulfillment System.

This script manages test runs, automatically saves test data, and allows
rerunning failed tests with the same parameters.
"""

import argparse
import json
import os
import subprocess
import sys
from datetime import datetime
from pathlib import Path


class TestRunner:
    def __init__(self, auth_token, docker_image="fulfillment-system", test_dir="tests"):
        self.auth_token = auth_token
        self.docker_image = docker_image
        self.test_dir = Path(test_dir)
        self.test_dir.mkdir(exist_ok=True)
    
    def run_new_test(self, rate_ms=500, min_pickup_ms=4000, max_pickup_ms=8000, seed=None):
        """Run a new test and save the test data."""
        # Generate test filename based on timestamp
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        test_file = self.test_dir / f"test_{timestamp}.json"
        
        print(f"Running new test and saving to: {test_file}")
        
        # Build docker command with --save-test
        cmd = [
            "docker", "run", "--rm",
            self.docker_image,
            self.auth_token,
            str(rate_ms),
            str(min_pickup_ms),
            str(max_pickup_ms),
        ]
        if seed is not None:
            cmd.append(str(seed))
        cmd.extend(["--save-test", "/tmp/test_data.json"])
        
        # Run docker and capture output
        try:
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                check=False
            )
            
            # Copy saved test file from container if it exists
            # Actually, we need to mount a volume or use docker cp
            # For now, let's modify the approach to save on the host
            # We'll mount the test directory as a volume
            
            # Re-run with volume mount
            cmd_with_volume = [
                "docker", "run", "--rm",
                "-v", f"{os.path.abspath(self.test_dir)}:/tests",
                self.docker_image,
                self.auth_token,
                str(rate_ms),
                str(min_pickup_ms),
                str(max_pickup_ms),
            ]
            if seed is not None:
                cmd_with_volume.append(str(seed))
            cmd_with_volume.extend(["--save-test", f"/tests/test_{timestamp}.json"])
            
            result = subprocess.run(
                cmd_with_volume,
                capture_output=True,
                text=True,
                check=False
            )
            
            print(result.stdout)
            if result.stderr:
                print(result.stderr, file=sys.stderr)
            
            test_file = self.test_dir / f"test_{timestamp}.json"
            
            # Parse result
            result_line = None
            for line in result.stdout.split('\n'):
                if line.startswith("RESULT: "):
                    result_line = line.replace("RESULT: ", "").strip()
                    break
            
            # Load and update test file with result
            if test_file.exists():
                with open(test_file, 'r') as f:
                    test_data = json.load(f)
                test_data['result'] = result_line
                test_data['timestamp'] = timestamp
                with open(test_file, 'w') as f:
                    json.dump(test_data, f, indent=2)
                
                print(f"\nTest saved to: {test_file}")
                print(f"Test ID: {test_data.get('testId', 'N/A')}")
                print(f"Result: {result_line}")
                
                return result_line, test_file
            else:
                print(f"Warning: Test file not found at {test_file}")
                return result_line, None
                
        except subprocess.CalledProcessError as e:
            print(f"Error running test: {e}", file=sys.stderr)
            print(e.stdout, file=sys.stdout)
            print(e.stderr, file=sys.stderr)
            return None, None
    
    def rerun_test(self, test_file):
        """Rerun a previously saved test."""
        test_file = Path(test_file)
        if not test_file.exists():
            print(f"Error: Test file not found: {test_file}", file=sys.stderr)
            return None
        
        print(f"Rerunning test from: {test_file}")
        
        # Build docker command with --load-test and --skip-submission
        # (Tests can only be submitted once, so skip submission when rerunning)
        cmd = [
            "docker", "run", "--rm",
            "-v", f"{os.path.abspath(test_file.parent)}:/tests",
            self.docker_image,
            "--load-test",
            f"/tests/{test_file.name}",
            self.auth_token,
            "--skip-submission"
        ]
        
        try:
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                check=False
            )
            
            print(result.stdout)
            if result.stderr:
                print(result.stderr, file=sys.stderr)
            
            # Parse result
            result_line = None
            for line in result.stdout.split('\n'):
                if line.startswith("RESULT: "):
                    result_line = line.replace("RESULT: ", "").strip()
                    break
            
            # Update test file with rerun info (but keep original result if skipping submission)
            with open(test_file, 'r') as f:
                test_data = json.load(f)
            
            if result_line and "skipped" not in result_line.lower():
                # Only update result if we got an actual result (not skipped)
                test_data['result'] = result_line
            test_data['rerun_timestamp'] = datetime.now().strftime("%Y%m%d_%H%M%S")
            test_data['rerun_result'] = result_line or "skipped (not submitted)"
            with open(test_file, 'w') as f:
                json.dump(test_data, f, indent=2)
            
            print(f"\nTest rerun completed. File: {test_file}")
            if result_line:
                print(f"Rerun result: {result_line}")
                if "skipped" in result_line.lower():
                    print("(Submission skipped - tests can only be submitted once to the server)")
            else:
                print("(No result line found in output)")
            
            return result_line
            
        except Exception as e:
            print(f"Error rerunning test: {e}", file=sys.stderr)
            return None
    
    def list_tests(self, show_all=False):
        """List all saved tests."""
        tests = sorted(self.test_dir.glob("test_*.json"))
        
        if not tests:
            print("No saved tests found.")
            return
        
        print(f"\nFound {len(tests)} saved test(s):\n")
        print(f"{'#':<4} {'Test File':<30} {'Test ID':<15} {'Result':<20} {'Timestamp':<20}")
        print("-" * 95)
        
        for i, test_file in enumerate(tests, 1):
            try:
                with open(test_file, 'r') as f:
                    test_data = json.load(f)
                test_id = test_data.get('testId', 'N/A')
                result = test_data.get('result', 'N/A')
                timestamp = test_data.get('timestamp', 'N/A')
                
                if show_all or not result or 'fail' in result.lower():
                    print(f"{i:<4} {test_file.name:<30} {test_id:<15} {result[:18]:<20} {timestamp:<20}")
            except Exception as e:
                print(f"{i:<4} {test_file.name:<30} {'ERROR':<15} {'Failed to load':<20} {'N/A':<20}")
    
    def run_continuous(self, max_tests=None, rate_ms=500, min_pickup_ms=4000, max_pickup_ms=8000):
        """Run tests continuously until a failure or max_tests is reached."""
        print("Running tests continuously...")
        print(f"Will stop on failure or after {max_tests if max_tests else 'unlimited'} tests\n")
        
        test_count = 0
        failed_tests = []
        
        while max_tests is None or test_count < max_tests:
            test_count += 1
            print(f"\n{'='*60}")
            print(f"Test #{test_count}")
            print(f"{'='*60}\n")
            
            result, test_file = self.run_new_test(rate_ms, min_pickup_ms, max_pickup_ms)
            
            if result is None:
                print(f"Test #{test_count} failed to run")
                break
            
            if 'fail' in result.lower():
                print(f"\n❌ Test #{test_count} FAILED!")
                if test_file:
                    failed_tests.append((test_count, test_file))
                break
            else:
                print(f"\n✅ Test #{test_count} PASSED")
        
        print(f"\n{'='*60}")
        print(f"Completed {test_count} test(s)")
        if failed_tests:
            print(f"\nFailed test(s):")
            for test_num, test_file in failed_tests:
                print(f"  Test #{test_num}: {test_file}")


def main():
    parser = argparse.ArgumentParser(description="Test runner for Cloud Kitchen Fulfillment System")
    parser.add_argument("auth_token", help="Authentication token for the challenge server")
    parser.add_argument("--docker-image", default="fulfillment-system", 
                       help="Docker image name (default: fulfillment-system)")
    parser.add_argument("--test-dir", default="tests", 
                       help="Directory to store test files (default: tests)")
    
    subparsers = parser.add_subparsers(dest="command", help="Command to execute")
    
    # Run new test
    parser_new = subparsers.add_parser("new", help="Run a new test")
    parser_new.add_argument("--rate-ms", type=int, default=500, 
                           help="Order placement rate in milliseconds (default: 500)")
    parser_new.add_argument("--min-pickup-ms", type=int, default=4000,
                           help="Minimum pickup time in milliseconds (default: 4000)")
    parser_new.add_argument("--max-pickup-ms", type=int, default=8000,
                           help="Maximum pickup time in milliseconds (default: 8000)")
    parser_new.add_argument("--seed", type=int, help="Seed for reproducible test")
    
    # Rerun test
    parser_rerun = subparsers.add_parser("rerun", help="Rerun a saved test")
    parser_rerun.add_argument("test_file", help="Test file to rerun")
    
    # List tests
    parser_list = subparsers.add_parser("list", help="List saved tests")
    parser_list.add_argument("--all", action="store_true", 
                            help="Show all tests (default: show only failed/unfinished)")
    
    # Run continuously
    parser_continuous = subparsers.add_parser("continuous", help="Run tests continuously")
    parser_continuous.add_argument("--max-tests", type=int, 
                                  help="Maximum number of tests to run")
    parser_continuous.add_argument("--rate-ms", type=int, default=500,
                                  help="Order placement rate in milliseconds (default: 500)")
    parser_continuous.add_argument("--min-pickup-ms", type=int, default=4000,
                                  help="Minimum pickup time in milliseconds (default: 4000)")
    parser_continuous.add_argument("--max-pickup-ms", type=int, default=8000,
                                  help="Maximum pickup time in milliseconds (default: 8000)")
    
    args = parser.parse_args()
    
    if not args.command:
        parser.print_help()
        sys.exit(1)
    
    runner = TestRunner(args.auth_token, args.docker_image, args.test_dir)
    
    if args.command == "new":
        runner.run_new_test(args.rate_ms, args.min_pickup_ms, args.max_pickup_ms, args.seed)
    elif args.command == "rerun":
        runner.rerun_test(args.test_file)
    elif args.command == "list":
        runner.list_tests(args.all)
    elif args.command == "continuous":
        runner.run_continuous(args.max_tests, args.rate_ms, args.min_pickup_ms, args.max_pickup_ms)


if __name__ == "__main__":
    main()
