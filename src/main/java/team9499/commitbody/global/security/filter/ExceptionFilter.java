package team9499.commitbody.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import team9499.commitbody.global.Exception.JwtTokenException;
import team9499.commitbody.global.Exception.SecurityException;
import team9499.commitbody.global.payload.ErrorResponse;

import java.io.IOException;

@Slf4j
public class ExceptionFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request,response);
        }catch (JwtTokenException e){
            setErrorResponse(response,e.getMessage(),e.getExceptionStatus());
        }catch (SecurityException e){
            setErrorResponse(response,e.getMessage(),e.getExceptionStatus());
        }
    }

    private void setErrorResponse(HttpServletResponse response, String message, int status){

        try{
            ObjectMapper objectMapper = new ObjectMapper();
            response.setCharacterEncoding("UTF-8");
            response.setStatus(status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse errorResponse = new ErrorResponse(false, message);
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
