//package uk.ac.ebi.reactome.core.controller;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URL;
//import java.net.URLConnection;
///**
// * Created by flo on 6/13/14.
// */
//public class PhpPostConnect {
//
//
//
//        /** Contains the URL to the PHP-Script */
//        private URL path;
//        /** The Connection to the URL */
//        private URLConnection con;
//
//
//
//        /**
//         * Construct which also define the targed URL
//         * @param path The URL to the target PHP-Script
//         */
//        public PhpPostConnect(URL path) {
//            this.path = path;
//        }
//
//        /**
//         * Set the URL to the target PHP-Script
//         * @param sitepath The URL to the target PHP-Script
//         */
//        public void setSitePath(URL sitepath) {
//            this.path = sitepath;
//        }
//
//        /**
//         * To get the target-URL
//         * @return The URL to the target PHP-Script
//         */
//        public URL getSitePath() {
//            return path;
//        }
//
//
//        /**
//         * Reading incoming data from the target-URL
//         * @return The incoming data
//         * @throws IOException
//         */
//        public String read() throws IOException {
//            if (con == null) {
//                con = path.openConnection();
//            }
//            InputStream in = con.getInputStream();
//            int c = 0;
//            StringBuffer incoming = new StringBuffer();
//            while (c >= 0) {
//                c = in.read();
//                incoming.append((char) c);
//            }
//            String html =  incoming.toString();
//            return html.replace(html.substring(html.length()-1), "");
//        }
//
//    }
//
//
//
