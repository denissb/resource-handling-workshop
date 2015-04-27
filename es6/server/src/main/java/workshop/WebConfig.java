package workshop;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.springmvc.HandlebarsViewResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.servlet.resource.VersionResourceResolver;

import java.io.IOException;
import java.util.Collections;

@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {

    @Autowired
    private Environment env;

    @Value("${resources.projectroot:}")
    private String projectRoot;

    @Value("${app.version:}")
    private String appVersion;

    // return a constant string in development for our debugger and web developer tools
    private String getApplicationVersion() {
        return this.env.acceptsProfiles("development") ? "dev" : this.appVersion;
    }

    private String getProjectRootRequired() {
        Assert.state(this.projectRoot != null, "Please set \"resources.projectRoot\" in application.properties");
        return this.projectRoot;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        boolean devMode = this.env.acceptsProfiles("development");
        String version = getApplicationVersion();
        String location = devMode ? "file:///" + getProjectRootRequired() + "/client/src/" : "classpath:static/";
        boolean useResourceCache = !devMode;

        VersionResourceResolver versionResolver = new VersionResourceResolver()
                .addFixedVersionStrategy(version, "/**/*.js", "/**/*.map")
                .addContentVersionStrategy("/**");

        registry.addResourceHandler("/**")
                .addResourceLocations(location)
                .resourceChain(useResourceCache)
                .addResolver(versionResolver);
    }


    @Bean
    @Autowired
    public HandlebarsViewResolver handlebarsViewResolver(ResourceUrlProvider urlProvider) {
        HandlebarsViewResolver resolver = new HandlebarsViewResolver();
        resolver.setPrefix("classpath:/templates/");
        resolver.registerHelper("src", new ResourceUrlHelper(urlProvider));
        resolver.setCache(!this.env.acceptsProfiles("development"));
        resolver.setFailOnMissingFile(false);
        resolver.setAttributesMap(Collections.singletonMap("applicationVersion", getApplicationVersion()));
        return resolver;
    }

    class ResourceUrlHelper implements Helper<String> {

        private final ResourceUrlProvider resourceUrlProvider;

        public ResourceUrlHelper(ResourceUrlProvider resourceUrlProvider) {
            this.resourceUrlProvider = resourceUrlProvider;
        }

        @Override
        public CharSequence apply(String context, Options options) throws IOException {
            // rewrite the given URL using Spring's ResourceUrlProvider
            return this.resourceUrlProvider.getForLookupPath(context);
        }
    }

}