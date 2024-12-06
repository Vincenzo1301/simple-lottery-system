# Lottery Drawing System

This repository contains my solution to a task from the *Distributed Systems Programming* lecture. The task involves
developing a simple yet complete system for managing lottery drawings, including server and client functionalities.

---

## 📖 **Overview**

The system is designed to manage lottery drawings as follows:

- A **server** draws a random number between 0 and 255 every hour on the hour (e.g., 13:00, 14:00).
- Users can **register for specific drawing slots**, specifying:
    - The date and time of the drawing(s).
    - Their guessed number.
    - An email address for receiving the results.

---

## 💡 **Key Features**

1. **Drawing Registration**
    - Users register for one or more drawing slots.
    - Each registration costs 100 SEK (added to the pool).
    - Duplicate registrations for the same number in a slot are disallowed.

2. **Winning Calculations**
    - The server distributes the pool equally among winners.
    - If there are no winners, the pool is carried over to the next drawing.

3. **Notifications**
    - Winners are notified via an email (or logged to the console in the basic implementation).

4. **Historical Data**
    - Users can retrieve results of previous drawings, including:
        - The drawn number.
        - The number of winners.
        - Total winnings per drawing.

---

## ⚙️ **Functionality**

### **Core Operations**

1. **Drawing Registration**
    - **Request**: User provides:
        - Drawing slot(s) (date and time).
        - Guessed number.
        - Email address.
    - **Response**:
        - *Success*: Registration is accepted.
        - *Error*: For invalid registrations, such as:
            - Attempting to register for past drawings.
            - Guessing a number out of range.
            - Duplicate registrations for the same slot and number.

2. **Retrieve Historical Data**
    - **Request**: User specifies the time period of interest.
    - **Response**:
        - Results for the specified period, including:
            - Drawn numbers.
            - Number of winners.
            - Total winnings.
        - *Error*: For invalid time periods (e.g., start date is after the end date).

---

## 🔧 **Implementation Details**

1. **Basic Implementation**
    - Server and client communicate via single request/reply messages (e.g., using TCP or UDP).
    - Data format can be raw or serialized (e.g., JSON).
    - Emails are simulated by logging notifications to the console.

2. **Optional Part 1** (0.5 bonus points)
    - The server is implemented as a REST API using JSON for communication.
    - Appropriate HTTP methods are used (e.g., `POST` for drawing registration).

3. **Optional Part 2** (0.5 bonus points)
    - Actual email notifications are implemented.
    - For example, integration with the Google Mail REST API.

---

## 🛠️ **Technologies Used**

- Programming Language: [Your chosen language here]
- Communication: [TCP/UDP or REST API]
- Data Format: JSON

---

## 📜 **Limitations**

- **Security**: The system does not account for dishonest users (e.g., those attempting to bet on all numbers).
- **Scalability**: Designed for simplicity, not large-scale deployment.

---

## 📨 **Contact**

If you have any questions about this implementation, feel free to reach out or review the provided codebase for detailed
insights.  