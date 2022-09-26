package com.example.Bar.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Provider {
    private String name;
    private List<Position> positions;
}
