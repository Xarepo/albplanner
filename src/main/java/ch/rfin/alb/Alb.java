package ch.rfin.alb;

import java.util.Map;
import java.util.stream.Stream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Provides easy access to the different tools.
 * @author Christoffer Fink
 */
public class Alb {

    public static AlbInstance instance() {
        return AlbInstance.albInstance();
    }

    public static AlbBuilder builder() {
        return AlbBuilder.albBuilder();
    }

    /**
     * Returns a default ALB parser.
     */
    public static AlbParser<AlbInstance> parser() {
        return new DefaultAlbParser();
    }

    /**
     * Use a default ALB parser to parse a string such as the contents of an
     * ALB file.
     * @param instance a string representing the contents of an ALB file
     * @return the parsed AlbInstance
     */
    public static AlbInstance parseString(String instance) {
        return parser().parseString(instance);
    }

    /**
     * Use a default ALB parser to parse an ALB file specified as a file
     * name/path.
     * @param file a file name or path
     * @return the parsed AlbInstance
     */
    public static AlbInstance parseFile(String file) throws IOException {
        return parser().parseFile(file);
    }

    /**
     * Use a default ALB parser to parse an ALB file specified as a file path.
     * @param file a file path
     * @return the parsed AlbInstance
     */
    public static AlbInstance parse(Path file) throws IOException {
        return parser().parse(file);
    }

    /**
     * Use a default ALB parser to parse a stream of lines such as from an
     * ALB file.
     * @param lines a stream of lines representing the contents of an ALB file
     * @return the parsed AlbInstance
     */
    public static AlbInstance parse(Stream<String> lines) {
        return parser().parse(lines);
    }

}
