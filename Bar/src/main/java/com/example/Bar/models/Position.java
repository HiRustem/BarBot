package com.example.Bar.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Position {
    private String name;
    private Integer quantity;
    private String type;
}
