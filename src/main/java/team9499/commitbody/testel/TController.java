package team9499.commitbody.testel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class TController {

    @GetMapping("/v1")
    public String v1(){
        return "test";
    }
}
