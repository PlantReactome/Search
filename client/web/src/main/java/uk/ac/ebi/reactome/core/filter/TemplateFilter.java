package uk.ac.ebi.reactome.core.filter;

import org.apache.commons.io.IOUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class TemplateFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // create a char wrapper to the response
        CharResponseWrapper wrapper = new CharResponseWrapper((HttpServletResponse) response);

        // do the filter in order to get the response into the wrapper
        chain.doFilter(request, wrapper);

        // get the frontier template and write it together with the response into a char writer
        CharArrayWriter caw = new CharArrayWriter();

        // insert the header
        caw.write(getHeader());
        // insert the response (i.e. the JSP)
        caw.write(wrapper.toString());
        // insert the footer
        caw.write(getFooter());

        // put the char writer content into the response/wrapper output
        response.setContentLength(caw.toString().length());
        // get a writer to write into the response output

        PrintWriter out = response.getWriter();
        out.write(caw.toString());
        out.close();
    }

    @Override
    public void destroy() {

    }

    private String getHeader() throws IOException {
        try {
            URL url = new URL("http://dev2.reactome.org/common/header.php");
            return IOUtils.toString(url.openConnection().getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return String.format("<span style='color:red'>%s</span>", e.getMessage());
        }
    }

    private String getFooter() throws IOException {
        try {
            URL url = new URL("http://dev2.reactome.org/common/footer.php");
            return IOUtils.toString(url);
        } catch (IOException e) {
            e.printStackTrace();
            return String.format("<span style='color:red'>%s</span>", e.getMessage());
        }
    }
}
