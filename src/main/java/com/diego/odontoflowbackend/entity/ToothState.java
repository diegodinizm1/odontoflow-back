package com.diego.odontoflowbackend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * State of a single tooth in the odontogram (stored as JSONB).
 * e.g. { "condition": "CARIES", "surfaces": ["O", "M"] }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToothState {
    private String condition;
    private List<String> surfaces = new ArrayList<>();
}
