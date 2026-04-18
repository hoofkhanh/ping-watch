# Purpose

This application is built to solve a common problem: **reliably monitoring deployed APIs over time without depending on a local or host machine**.

In many cases, running health checks directly on the same server as the API is not reliable, as the host environment may differ from real-world conditions (network, configuration, dependencies, etc.). Similarly, using a personal machine to run checks continuously is not practical, as it requires keeping your computer running at all times.

This application provides a centralized solution to:

- Continuously monitor API availability over long periods (e.g., 24/7)
- Detect and surface failures as soon as they occur
- Observe API behavior from an external, more realistic environment
- Access monitoring results from any device without needing to keep a machine running

It is designed for scenarios where **consistent, external, and long-running API health monitoring** is required without missing critical failures.

# BACKEND

This product helps monitor your APIs continuously.
This project is intended to be deployed on GCP (Google Cloud Platform).

Database Diagram: https://drive.google.com/file/d/1GW6p7MYtnBFIcIuuNxkqknya9Uyu4SuI/view?usp=sharing

Related repositories:

- Frontend: https://github.com/hoofkhanh/ping-watch-ui
- Tooling (SonarQube): https://github.com/hoofkhanh/ping-watch-tooling
