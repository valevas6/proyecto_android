package com.example.ride_bit.Model;

public class Driver {

    private String name, email, phone, password, carPlade;

    public Driver(){

    }

    public Driver(String name, String email, String phone, String password, String carPlade) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.carPlade = carPlade;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCarPlade() {
        return carPlade;
    }

    public void setCarPlade(String carPlade) {
        this.carPlade = carPlade;
    }

    @Override
    public String toString() {
        return "Driver{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", password='" + password + '\'' +
                ", carPlade='" + carPlade + '\'' +
                '}';
    }
}
