<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@taglib prefix="mytag" uri="/WEB-INF/tags/customTag.tld"%>

<c:import url="header.jsp"/>
<div class="ebi-content" >

  <div class="grid_23 padding">
    <h3>${entry.name}
      <c:if test="${not empty entry.accession}">
        <span> (${entry.accession})</span>
      </c:if>
    </h3>
  </div>

  <div class="grid_23  padding">
      <!-- INTERACTORS TABLE -->
      <c:if test="${not empty entry.interactions}">
        <div class="grid_23  padding">
          <h5>Interactions</h5>
          <div class="wrap">
            <table class="fixedTable">
              <thead>
              <tr class="tableHead">
                <td style="width: 6%">Interaction Score</td>
                <td style="width: 6%">Interaction ID</td>
                <td style="width: 6%">Interactor Accession</td>
                <td>Reactome Entry</td>
              </tr>
              </thead>
            </table>
            <div class="inner_table_div" style="height: 400px;">
              <table>
                <c:forEach var="interaction" items="${entry.interactions}">
                  <tr>
                    <td  style="width: 6%">${interaction.score}</td>

                    <td style="width: 6%">
                      <a href="${fn:replace(intactUrl, '##ID##', interaction.interactionId)}"
                         title="Show ${interaction.interactionId}"
                         rel="nofollow">${interaction.interactionId}</a>
                      </td>
                    <td style="width: 6%">${interaction.accession}</td>
                    <td>
                      <c:forEach var="reactomeEntry" items="${interaction.interactorReactomeEntries}">
                        <ul  class="list overflowList">
                          <li>
                            <a href="/detail/${reactomeEntry.reactomeId} " class="" title="Show Details" rel="nofollow">${reactomeEntry.reactomeName}<span> (${reactomeEntry.reactomeId})</span></a>
                          </li>
                        </ul>
                      </c:forEach>
                    </td>
                  </tr>
                </c:forEach>
              </table>
            </div>
          </div>
        </div>
      </c:if>
  </div>


</div>
<div class="clear"></div>

</div>            <%--A weird thing to avoid problems--%>
<c:import url="footer.jsp"/>