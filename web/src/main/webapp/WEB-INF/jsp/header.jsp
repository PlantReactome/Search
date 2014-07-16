<!doctype html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<html lang="en-US">
<head>
    <meta charset="UTF-8">
    <title>
        Reactome | Welcome to Reactome Pathway Database</title>
    <link href="http://reactomedev.oicr.on.ca/wordpress/wp-content/themes/HS_OICR_2013/960_24_col.css" rel="stylesheet" type="text/css">
    <link href="http://reactomedev.oicr.on.ca/wordpress/wp-content/themes/HS_OICR_2013/reset.css" rel="stylesheet" type="text/css">
    <link href="http://reactomedev.oicr.on.ca/wordpress/wp-content/themes/HS_OICR_2013/text.css" rel="stylesheet" type="text/css">
    <link rel="stylesheet" type="text/css" media="all" href="http://reactomedev.oicr.on.ca/wordpress/wp-content/themes/HS_OICR_2013/style.css">
    <link href="http://reactomedev.oicr.on.ca/wordpress/wp-content/themes/HS_OICR_2013/buttons.css" rel="stylesheet" type="text/css">
    <script>!function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0],p=/^http:/.test(d.location)?'http':'https';if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src=p+"://platform.twitter.com/widgets.js";fjs.parentNode.insertBefore(js,fjs);}}(document,"script","twitter-wjs");</script>
    <link rel="pingback" href="http://reactomedev.oicr.on.ca/wordpress/xmlrpc.php">

    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/main.css" type="text/css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/ebi-fluid.css" type="text/css" media="screen">

    <!--[if lt IE 9]>
    <script src="http://reactomedev.oicr.on.ca/wordpress/wp-content/themes/HS_OICR_2013/js/html5.js" type="text/javascript"></script>
    <![endif]-->
    <meta name='robots' content='noindex,follow' />
    <script type='text/javascript' src='http://reactomedev.oicr.on.ca/wordpress/wp-includes/js/jquery/jquery.js?ver=1.11.0'></script>
    <script type='text/javascript' src='http://reactomedev.oicr.on.ca/wordpress/wp-includes/js/jquery/jquery-migrate.min.js?ver=1.2.1'></script>
    <link rel="EditURI" type="application/rsd+xml" title="RSD" href="http://reactomedev.oicr.on.ca/wordpress/xmlrpc.php?rsd" />
    <link rel="wlwmanifest" type="application/wlwmanifest+xml" href="http://reactomedev.oicr.on.ca/wordpress/wp-includes/wlwmanifest.xml" />
    <link rel='prev' title='Documentation' href='http://reactomedev.oicr.on.ca/?page_id=1164' />
    <link rel='next' title='Content' href='http://reactomedev.oicr.on.ca/?page_id=1191' />
    <meta name="generator" content="WordPress 3.9.1" />
    <link rel='canonical' href='http://reactomedev.oicr.on.ca/' />
    <link rel='shortlink' href='http://reactomedev.oicr.on.ca/' />
    <style type="text/css" media="all">
        /* <![CDATA[ */
        @import url("http://reactomedev.oicr.on.ca/wordpress/wp-content/plugins/wp-table-reloaded/css/plugin.css?ver=1.9.4");
        @import url("http://reactomedev.oicr.on.ca/wordpress/wp-content/plugins/wp-table-reloaded/css/tablesorter.css?ver=1.9.4");
        /* ]]> */
    </style>
    <!-- Autocompleter needed style -->
    <style>
        .autocomplete-suggestions {
            border: 1px solid gray;
            background: gainsboro;
            overflow-y: scroll;
            font-size: 85%;
        }
        .autocomplete-suggestion {
            padding: 3px 7px;
            white-space: nowrap;
            cursor:pointer;
        }
        .autocomplete-selected {
            background: #F0F0F0;
        }
        .autocomplete-suggestions strong {
            font-weight: bold;
            color: #05194C;
        }
    </style>

</head>

<body class="">
<div class="container_24">
    <!--header-->
    <div class="header">

        <div class="grid_24"><!--Reactome logo-->
            <a href="http://reactomedev.oicr.on.ca/"><img src="http://reactomedev.oicr.on.ca/wordpress/wp-content/themes/HS_OICR_2013/images/ReactomeLogo.png" alt="Reactome: A Curated Pathway Database"></a>
        </div><!--end grid 24 Reactome logo-->

        <div class="clear"></div><!--clear the row with logo-->

        <div class="navwrapper">
            <div class="grid_24">
                <!--nav-->
                <div class="nav">
                    <div class="menu-main-container"><ul id="menu-main" class="menu"><li id="menu-item-1275" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-has-children menu-item-1275"><a href="/?page_id=1206">About</a>
                        <ul class="sub-menu">
                            <li id="menu-item-1065" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-1065"><a href="/?page_id=5">About Reactome</a></li>
                            <li id="menu-item-1245" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-1245"><a href="/?page_id=2">News</a></li>
                            <li id="menu-item-1060" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-1060"><a href="/?page_id=581">Reactome Team</a></li>
                            <li id="menu-item-1059" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-1059"><a href="/?page_id=591">Scientific Advisory Board</a></li>
                            <li id="menu-item-1056" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-1056"><a href="/?page_id=298">Other Reactomes</a></li>
                            <li id="menu-item-1053" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-1053"><a href="/?page_id=362">License Agreement</a></li>
                            <li id="menu-item-1057" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-1057"><a href="/?page_id=305">Reactome Disclaimer</a></li>
                        </ul>
                    </li>
                        <li id="menu-item-1260" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-has-children menu-item-1260"><a href="/?page_id=1191">Content</a>
                            <ul class="sub-menu">
                                <li id="menu-item-1067" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-1067"><a href="/cgi-bin/toc?DB=test_reactome_48">Table of Contents</a></li>
                                <li id="menu-item-1068" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-1068"><a href="/cgi-bin/doi_toc?DB=test_reactome_48">DOIs</a></li>
                                <li id="menu-item-1069" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-1069"><a href="/cgi-bin/classbrowser?DB=test_reactome_48">Data Schema</a></li>
                                <li id="menu-item-1190" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-1190"><a href="http://wiki.reactome.org/index.php/Reactome_Calendar">Editorial Calendar</a></li>
                                <li id="menu-item-1072" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-1072"><a href="/stats.html">Statistics</a></li>
                            </ul>
                        </li>
                        <li id="menu-item-1254" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-has-children menu-item-1254"><a href="/?page_id=1164">Documentation</a>
                            <ul class="sub-menu">
                                <li id="menu-item-1074" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-1074"><a href="http://wiki.reactome.org/index.php/Usersguide">User Guide</a></li>
                                <li id="menu-item-1051" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-1051"><a href="/?page_id=364">Data Model</a></li>
                                <li id="menu-item-1052" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-1052"><a href="/?page_id=370">Orthology Prediction</a></li>
                                <li id="menu-item-1055" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-1055"><a href="/?page_id=378">Object/Relational Mapping</a></li>
                                <li id="menu-item-1075" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-1075"><a href="http://wiki.reactome.org/index.php/Main_Page">Wiki</a></li>
                                <li id="menu-item-1054" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-1054"><a href="/?page_id=381">Linking to Reactome</a></li>
                                <li id="menu-item-1421" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-1421"><a href="/?page_id=1418">Referencing Reactome</a></li>
                            </ul>
                        </li>
                        <li id="menu-item-1272" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-has-children menu-item-1272"><a href="/?page_id=1266">Tools</a>
                            <ul class="sub-menu">
                                <li id="menu-item-1083" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-1083"><a href="/PathwayBrowser/">Pathway Browser</a></li>
                                <li id="menu-item-1088" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-1088"><a href="/PathwayBrowser/#TOOL=AT">Analyze Data</a></li>
                                <li id="menu-item-1084" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-1084"><a href="/PathwayBrowser/#TOOL=AT">Species Comparison</a></li>
                                <li id="menu-item-1082" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-1082"><a href="http://wiki.reactome.org/index.php/Reactome_FI_Cytoscape_Plugin">Reactome FI Network</a></li>
                                <li id="menu-item-1078" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-1078"><a href="${pageContext.request.contextPath}/advanced">Advanced Search</a></li>
                                <li id="menu-item-1079" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-1079"><a href="/cgi-bin/small_molecule_search?DB=test_reactome_48">Small Molecule Search</a></li>
                                <li id="menu-item-1080" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-1080"><a href="/cgi-bin/mart">BioMart</a></li>
                            </ul>
                        </li>
                        <li id="menu-item-1196" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-has-children menu-item-1196"><a href="/?page_id=1194">Community</a>
                            <ul class="sub-menu">
                                <li id="menu-item-1058" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-1058"><a href="/?page_id=402">Reactome Outreach</a></li>
                                <li id="menu-item-1401" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-1401"><a href="/?page_id=1388">Reactome Events</a></li>
                                <li id="menu-item-1397" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-1397"><a href="/?page_id=1381">Reactome Training</a></li>
                                <li id="menu-item-1404" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-1404"><a href="/?page_id=1001">Reactome Publications</a></li>
                                <li id="menu-item-1090" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-1090"><a href="http://www.mendeley.com/groups/2805251/reactome/papers/">Papers Citing Reactome</a></li>
                                <li id="menu-item-1398" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-1398"><a href="/?page_id=1300">Resources Guide</a></li>
                                <li id="menu-item-1093" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-1093"><a href="https://lists.reactome.org/mailman/listinfo/reactome-announce">Mailing List</a></li>
                            </ul>
                        </li>
                        <li id="menu-item-1089" class="menu-item menu-item-type-custom menu-item-object-custom menu-item-1089"><a href="/download/index.html">Download</a></li>
                        <li id="menu-item-1429" class="menu-item menu-item-type-post_type menu-item-object-page menu-item-1429"><a href="/?page_id=328">Contact</a></li>
                    </ul></div>
                </div><!--close nav-->

                <div class="search_bar">
                    <form id="search_form" action="${pageContext.request.contextPath}/query" method="get">

                        <input id="local-searchbox" type="search" class="search" name="q" placeholder="e.g. O95631, NTN1, signaling by EGFR, glucose" value="${q}"/>
                        <c:choose>
                            <c:when test="${not empty species}">
                                <c:forEach var="item" items="${species}">
                                    <input type="hidden" name="species" value="${item}"/>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <input type="hidden" name="species" value="Homo sapiens"/>
                                <input type="hidden" name="species" value="Entries without species"/>
                            </c:otherwise>
                        </c:choose>
                        <input type="hidden" name="cluster" value="true"/>
                        <c:if test="${not empty types}">
                            <c:forEach var="item" items="${types}">
                                <input type="hidden" name="types" value="${item}"/>
                            </c:forEach>
                        </c:if>
                        <c:if test="${not empty compartments}">
                            <c:forEach var="item" items="${compartments}">
                                <input type="hidden" name="compartments" value="${item}"/>
                            </c:forEach>
                        </c:if>
                        <c:if test="${not empty keywords}">
                            <c:forEach var="item" items="${keywords}">
                                <input type="hidden" name="keywords" value="${item}"/>
                            </c:forEach>
                        </c:if>

                        <input type="submit" class="submit" value="Search">

                    </form>
                </div><!--close search-->

            </div><!--close grid 24-->
            <div class="clear"></div><!--clear nav row-->
        </div><!--close navwrapper-->
    </div><!--close header-->

    <div class="clear"></div><!--clear header-->
    <div style="background:gainsboro;border:1px solid gray; text-align:center" id="hideMe">
        <div style="font-size:small;padding:10px">
  <span style="font-size:larger;color:red">
   THIS SITE IS USED FOR SOFTWARE DEVELOPMENT AND TESTING
  </span><br/>
  <span style="font-size:smaller;color:red">
   IT IS NOT STABLE, IS LINKED TO AN INCOMPLETE DATA SET, AND IS NOT MONITORED FOR PERFORMANCE.
   WE STRONGLY RECOMMEND THE USE OF OUR <a href="http://www.reactome.org">PUBLIC SITE</a>
  </span>
        </div>
    </div>
    <!--content-->
    <div class="wrapper">