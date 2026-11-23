package ca.bc.gov.open.digitalformsapi.viirp.controller;

import org.springframework.util.StringUtils;

import ca.bc.gov.open.digitalformsapi.viirp.exception.BadRequestException;
import ca.bc.gov.open.digitalformsapi.viirp.model.RegistrationCreate;

final class RegistrationValidation {

    private RegistrationValidation() {
    }

    static void requireName(RegistrationCreate registration, String field) {
        if (!StringUtils.hasText(registration.getFirstGivenNm())
                && !StringUtils.hasText(registration.getSecondGivenNm())
                && !StringUtils.hasText(registration.getSurnameNm())
                && !StringUtils.hasText(registration.getOrganizationNm())) {
            throw new BadRequestException(field,
                    "Provide at least one nonblank name: firstGivenNm, secondGivenNm, surnameNm, or organizationNm.");
        }
    }
}
