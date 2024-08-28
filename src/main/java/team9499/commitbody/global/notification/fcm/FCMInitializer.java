package team9499.commitbody.global.notification.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Component
@Slf4j
public class FCMInitializer {

    @Value("${fcm.firebase_config_path}")
    private String firebaseConfigPath;

    @PostConstruct
    public void initialize(){
        try {
            GoogleCredentials googleCredentials = GoogleCredentials.
                    fromStream(new ClassPathResource(firebaseConfigPath).getInputStream());
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(googleCredentials)
                    .build();
            if (FirebaseApp.getApps().isEmpty()){
                FirebaseApp.initializeApp(options);
                log.info("FirebaseApp initialization complete");
            }
        }catch (IOException e){
            log.error(e.getMessage());
        }
    }
}
