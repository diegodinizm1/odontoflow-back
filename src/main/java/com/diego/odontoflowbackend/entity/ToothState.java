package com.diego.odontoflowbackend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Estado de um dente no odontograma (armazenado em JSONB).
 * Ex.: { "condition": "CARIES", "surfaces": ["O", "M"] }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToothState {
    private String condition;
    private List<String> surfaces = new ArrayList<>();
}
