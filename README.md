# COOPR8 API Documentation v1.0

## Base Information
**Base URL:** `https://coopr8.onrender.com/api/v1`

**Authentication:** JWT Bearer Token

**Default Headers:**
```
Content-Type: application/json
Authorization: Bearer <token>
```

## Table of Contents
- [Authentication](#authentication)
- [Admin](#admin)
- [Loans](#loans)
- [OTP](#otp)
- [Payments](#payments)
- [Savings](#savings)
- [Shares](#shares)
- [User](#user)
- [Webhooks](#webhooks)

---

## Authentication

### Register New User
**Method:** POST  
**Path:** `/auth/signup`

**Request Body:**
```json
{
  "firstName": "string",
  "lastName": "string",
  "email": "string",
  "password": "string",
  "membershipType": "string"
}
```

**Success Response:**
```json
{
  "token": "string",
  "message": "string",
  "user": {
    "id": "string",
    "firstName": "string",
    "lastName": "string",
    "email": "string",
    "role": "string"
  }
}
```

### User Login
**Method:** POST  
**Path:** `/auth/login`

**Request Body:**
```json
{
  "email": "string",
  "password": "string"
}
```

**Success Response:** Same as registration

### Get User Profile
**Method:** GET  
**Path:** `/auth/profile`

**Response:** Returns authenticated user's profile

### Change Password
**Method:** PUT  
**Path:** `/auth/changepass`

**Request Body:**
```json
{
  "currentPassword": "string",
  "newPassword": "string",
  "confirmPassword": "string"
}
```

---

## Admin

### Get All Users
**Method:** GET  
**Path:** `/admin/users`

**Response:** List of all users

### Find User by ID
**Method:** GET  
**Path:** `/admin/user/id/{userId}`

**Response:** Single user details

### Find User by Membership Number
**Method:** GET  
**Path:** `/admin/user/memship/{memship}`

**Response:** Single user details

### Get New Users
**Method:** GET  
**Path:** `/admin/users/new`

**Response:** List of unactivated users

### Activate User
**Method:** PUT  
**Path:** `/admin/user/{userId}/activate`

**Response:** Activated user object

### Bulk Create Users
**Method:** POST  
**Path:** `/admin/users/multi`

**Request Body:** Array of user objects  
**Response:** List of created users

---

## Loans

### Apply for Loan
**Method:** POST  
**Path:** `/loan/apply`

**Request Body:**
```json
{
  "amount": "number",
  "purpose": "string",
  "durationMonths": "integer"
}
```

### Get All Loans (Admin)
**Method:** GET  
**Path:** `/loan/all`

**Response:** List of all loans

### Get User Loans
**Method:** GET  
**Path:** `/loan/myloans`

**Response:** List of authenticated user's loans

### Approve Loan (Admin)
**Method:** PUT  
**Path:** `/loan/{loanId}/approve`

**Response:** Approved loan object

### Make Repayment
**Method:** POST  
**Path:** `/loan/{loanId}/repay`

**Request Body:**
```json
{
  "amount": "number",
  "paymentMethod": "string"
}
```

---

## OTP

### Send OTP
**Method:** POST  
**Path:** `/otp/sendOTP`

**Request Body:**
```json
{
  "email": "string",
  "purpose": "string"
}
```

### Validate OTP
**Method:** POST  
**Path:** `/otp/validateOTP`

**Request Body:**
```json
{
  "email": "string",
  "otp": "string"
}
```

---

## Payments

### Initialize Payment
**Method:** POST  
**Path:** `/payments/initialize`

**Request Body:** Paystack initialization request  
**Response:** Paystack payment link

### Verify Transaction
**Method:** GET  
**Path:** `/payments/verify/{reference}`

**Response:** Transaction verification details

---

## Savings

### Get All Savings (Admin)
**Method:** GET  
**Path:** `/savings/all`

**Response:** List of all savings

### Get User Savings
**Method:** GET  
**Path:** `/savings/mysavings`

**Response:** List of authenticated user's savings

### Get Last Saving
**Method:** GET  
**Path:** `/savings/last`

**Response:** User's most recent saving

---

## Shares

### Add Shares
**Method:** POST  
**Path:** `/shares/add`

**Request Body:** Shares details  
**Response:** Created shares record

### Withdraw Shares
**Method:** POST  
**Path:** `/shares/withdraw`

**Request Body:** Shares withdrawal details  
**Response:** Withdrawal status

### Approve Withdrawal (Admin)
**Method:** PUT  
**Path:** `/shares/{shareId}/approve`

**Response:** Approved withdrawal record

---

## User

### Update User Profile
**Method:** PUT  
**Path:** `/user/update`

**Request Body:** User details  
**Response:** Updated user object

### Find User by Phone
**Method:** GET  
**Path:** `/user/phone/{phone}`

**Response:** User details

---

## Webhooks

### Paystack Webhook
**Method:** POST  
**Path:** `/webhook/paystack`

**Request Body:** Paystack webhook payload  
**Response:** Processing status

---

## Error Responses

All endpoints may return:
- `401 Unauthorized` - Invalid/missing JWT
- `403 Forbidden` - Insufficient permissions
- `400 Bad Request` - Invalid input data
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error
