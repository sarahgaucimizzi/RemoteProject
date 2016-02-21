package com.sarahmizzi.fyp.classes;

/**
 * Created by Sarah on 17-Feb-16.
 */
public class User {
    String email;
    int age;
    String gender;

    public User(String email, int age, String gender) {
        this.email = email;
        this.age = age;
        this.gender = gender;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
