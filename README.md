# COOPR8 API Documentation

## Base Information
**Base URL**: `https://coopr8.onrender.com/api/v1`  
**Authentication**: JWT Bearer Token  
**Default Headers**:  
`Content-Type: application/json`  
`Authorization: Bearer <token>`  

## Authentication Endpoints
**Register New User**  
`POST /auth/signup`  
Request:
```json
{"firstName":"string","lastName":"string","email":"string","password":"string","membershipType":"string"}
Response:

json
{"token":"string","message":"string","user":{"id":"string","firstName":"string","lastName":"string","email":"string","role":"string"}}
User Login
POST /auth/login
Request:

json
{"email":"string","password":"string"}
Response: Same as registration

Get User Profile
GET /auth/profile
Response: Returns authenticated user's profile

Change Password
PUT /auth/changepass
Request:

json
{"currentPassword":"string","newPassword":"string","confirmPassword":"string"}
Admin Endpoints
Get All Users
GET /admin/users
Response:

json
[{"id":"string","firstName":"string","lastName":"string","email":"string","status":"string"}]
Find User by ID
GET /admin/user/id/{userId}
Response: Single user details

Activate User
PUT /admin/user/{userId}/activate
Response: Activated user object

Loan Endpoints
Apply for Loan
POST /loan/apply
Request:

json
{"amount":number,"purpose":"string","durationMonths":number}
Response:

json
{"loan":{"id":number,"amount":number,"status":"string"},"message":"string"}
Get User Loans
GET /loan/myloans
Response:

json
[{"id":number,"amount":number,"status":"string","createdAt":"timestamp"}]
OTP Endpoints
Send OTP
POST /otp/sendOTP
Request:

json
{"email":"string","purpose":"string"}
Payment Endpoints
Initialize Payment
POST /payments/initialize
Request:

json
{"amount":number,"email":"string"}
Savings Endpoints
Get User Savings
GET /savings/mysavings
Response:

json
[{"id":number,"amount":number,"date":"timestamp"}]
Error Handling
Common Status Codes:
400 - Bad Request
401 - Unauthorized
403 - Forbidden
404 - Not Found
500 - Server Error

Example Error:

json
{"timestamp":"ISO8601","status":number,"error":"string","message":"string","path":"string"}
Development
bash
git clone https://github.com/your-repo/coopr8.git
cd coopr8
mvn install
License
MIT License

text

This version:
1. Has all content in one continuous block
2. Uses compact formatting for easy copying
3. Maintains all essential documentation elements
4. Includes proper markdown formatting
5. Preserves all code blocks and examples

Simply copy this entire block and paste it directly into your GitHub README.md file.
