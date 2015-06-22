package feedpoller.stub;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleHandler extends AbstractHandler {
    private final Map<String, ResponseClass> handlerMap = new HashMap<>();

    private final List<String> requestedURLs = new ArrayList<>();


    /**
     * Tells this handler to serve the specified <param>responseAsString</file> when
     * a request is made for the specified <param>requestURL</param>.
     *
     * @param requestURL
     * @param responseAsString
     * @param responseCode
     */
    public void mapRequestToResponse(final String requestURL, final String responseAsString, final int responseCode) {
        handlerMap.put(requestURL, new ResponseClass(responseAsString, responseCode));
    }

    public void handle(@SuppressWarnings("unused") final String target, final HttpServletRequest httpServletRequest,
                       final HttpServletResponse httpServletResponse, final String responseBody) throws IOException,
            ServletException {

        httpServletResponse.setContentType("text/plain");
        httpServletResponse.getWriter().print(responseBody);
        ((Request) httpServletRequest).setHandled(true);
    }

    @Override
    public void handle(final String target, final Request request, final HttpServletRequest httpServletRequest,
                       final HttpServletResponse httpServletResponse)
            throws IOException, ServletException {

        String requestURL = httpServletRequest.getRequestURL().toString();
        final String queryString = httpServletRequest.getQueryString();

        if (queryString != null) {
            requestURL = requestURL + "?" + queryString;
        }

        final ResponseClass responseClass = handlerMap.get(requestURL);
        String responseAsString = responseClass.responseAsString;
        httpServletResponse.setStatus(responseClass.responseCode);

        handle(target, httpServletRequest, httpServletResponse, responseAsString);
    }

    public void clearHistory() {
        requestedURLs.clear();
    }

    public List<String> getHistory() {
        return requestedURLs;
    }

    class ResponseClass {
        public String responseAsString;
        public int responseCode;

        public ResponseClass(final String responseAsString, final int responseCode) {
            this.responseAsString = responseAsString;
            this.responseCode = responseCode;
        }
    }
}
