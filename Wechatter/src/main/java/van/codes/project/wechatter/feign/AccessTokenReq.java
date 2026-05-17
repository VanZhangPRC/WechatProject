package van.codes.project.wechatter.feign;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccessTokenReq {
    private String grant_type = "client_credential";
    private String appid;
    private String secret;

    public AccessTokenReq(String appid, String secret) {
        this.appid = appid;
        this.secret = secret;
    }
}