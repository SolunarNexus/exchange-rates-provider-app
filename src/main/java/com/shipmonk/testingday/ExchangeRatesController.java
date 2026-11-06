package com.shipmonk.testingday;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(
    path = "/api/v1/rates"
)
public class ExchangeRatesController
{

    @RequestMapping(method = RequestMethod.GET, path = "/{day}")
    public ResponseEntity<Object> getRates(@PathVariable("day") String day)
    {
        return new ResponseEntity<>(
            Map.of(),
            HttpStatus.OK
        );
    }

}
