package restfulapi.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RateLimitFilter implements Filter {

  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
  }

  @Override
  public void destroy() {
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    String clientIp = httpRequest.getRemoteAddr();
    // 클라이언트의 IP를 통해 bucket을 가져오거나 없는 경우 새로 생성
    Bucket bucket = buckets.computeIfAbsent(clientIp, k->makeNewBucket());
    if(bucket.tryConsume(1)){
      chain.doFilter(request,response);
    }else{
      httpResponse.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too Many Requests");
    }
  }

  protected Bucket makeNewBucket(){
    // 1분에 60개의 요청을 처리할 수 있도록 함
    Bandwidth limit = Bandwidth.classic(60, Refill.greedy(60, Duration.ofMinutes(1)));
    return Bucket4j.builder().addLimit(limit).build();
  }
}
