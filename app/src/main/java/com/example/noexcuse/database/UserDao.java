package com.example.noexcuse.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface UserDao {

    @Insert
    void insert(User user);

    @Query("SELECT * FROM User WHERE email = :email AND password = :password LIMIT 1")
    User login(String email, String password);
}