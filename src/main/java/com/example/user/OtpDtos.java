package com.example.user;

// ─── Send OTP Request ─────────────────────────────────────────────────────────
class SendOtpRequest {
    private String email;
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

// ─── Register with OTP Request ────────────────────────────────────────────────
class RegisterWithOtpRequest {
    private String name;
    private String email;
    private String number;
    private String password;
    private String otp;

    public String getName()     { return name; }
    public String getEmail()    { return email; }
    public String getNumber()   { return number; }
    public String getPassword() { return password; }
    public String getOtp()      { return otp; }

    public void setName(String name)         { this.name = name; }
    public void setEmail(String email)       { this.email = email; }
    public void setNumber(String number)     { this.number = number; }
    public void setPassword(String password) { this.password = password; }
    public void setOtp(String otp)           { this.otp = otp; }
}
