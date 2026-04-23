package com.example.noexcuse.database;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class User {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String firstName;
    public String lastName;
    public String email;
    public String password;
    public String birthDate; // format: "YYYY-MM-DD"

    public float weight; // kg
    public float height; // cm
}