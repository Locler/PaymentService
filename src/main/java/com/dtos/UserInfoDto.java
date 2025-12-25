package com.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDto implements Serializable {

    private Long id;

    private String name;

    private String surname;

    private String email;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private Boolean active;
}
