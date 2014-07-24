<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<ul class="tree">
<c:forEach var="node" items="${node.children}">
    <li><a href="${node.value.url}" class=""  title="Show Details" rel="nofollow">${node.key}</a>
    <c:set var="node" value="${node.value}" scope="request"/>
    <c:import url="/WEB-INF/jsp/node.jsp"/>
    </li>
</c:forEach>
</ul>
