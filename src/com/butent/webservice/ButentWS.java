package com.butent.webservice;

import com.butent.bee.server.utils.XmlUtils;
import com.butent.bee.shared.communication.ResponseObject;
import com.butent.bee.shared.data.SimpleRowSet;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.utils.BeeUtils;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

/**
 * This class was generated by the JAX-WS RI. JAX-WS RI 2.1.6 in JDK 6 Generated source version: 2.1
 * 
 */
@WebServiceClient(name = ButentWS.NAME, targetNamespace = ButentWS.NAMESPACE)
public class ButentWS extends Service {

  static final String NAMESPACE = "http://localhost/ButentWS/wsdl/";
  static final String PORT = "ButentWebServiceSoapPort";
  static final String NAME = "ButentWS";
  static final String ACTION = "http://localhost/ButentWS/action/ButentWebService";

  private static BeeLogger logger = LogUtils.getLogger(ButentWS.class);

  public static ResponseObject getPort(String address, String login, String password) {
    if (BeeUtils.anyEmpty(address, login, password)) {
      return ResponseObject.error("WebService address/login/password not defined");
    }
    logger.info("Connecting to webservice:", address);

    ButentWS butentWS;
    try {
      butentWS = new ButentWS(new URL(address));
    } catch (Exception e) {
      return ResponseObject.error(e);
    }
    ButentWebServiceSoapPort port = butentWS.getButentWebServiceSoapPort();

    ((BindingProvider) port).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
        butentWS.getWSDLDocumentLocation().toString());

    String answer = port.login(login, password);
    String error = "Unknown login response";

    if (BeeUtils.same(answer, "OK")) {
      error = null;
    }
    if (!BeeUtils.isEmpty(error)) {
      Map<String, String> messages = XmlUtils.getElements(answer, null);

      if (BeeUtils.containsKey(messages, "Message")) {
        error = messages.get("Message");

        if (BeeUtils.same(error, "Already logged in")) {
          error = null;
        }
      }
    }
    if (!BeeUtils.isEmpty(error)) {
      return ResponseObject.error(error);
    }
    return ResponseObject.response(port);
  }

  public static ResponseObject getSQLData(String address, String login, String password,
      String query, String[] columns) {

    ResponseObject response = getPort(address, login, password);

    if (response.hasErrors()) {
      return response;
    }
    logger.debug("GetSQLData:", query);

    String answer = ((ButentWebServiceSoapPort) response.getResponse())
        .process("GetSQLData", "<query>" + query + "</query>");

    SimpleRowSet data = new SimpleRowSet(columns);
    Node node = null;

    try {
      node = XmlUtils.fromString(answer).getFirstChild();

      if (BeeUtils.same(node.getLocalName(), "Error")) {
        return ResponseObject.error(node.getTextContent());
      }
    } catch (Exception e) {
      return ResponseObject.error(answer);
    }
    if (node.hasChildNodes()) {
      for (int i = 0; i < node.getChildNodes().getLength(); i++) {
        NodeList row = node.getChildNodes().item(i).getChildNodes();
        int c = row.getLength();

        String[] cells = new String[data.getNumberOfColumns()];

        for (int j = 0; j < c; j++) {
          cells[data.getColumnIndex(row.item(j).getLocalName())] = row.item(j).getTextContent();
        }
        data.addRow(cells);
      }
    }
    logger.debug("GetSQLData cols:", data.getNumberOfColumns(), "rows:", data.getNumberOfRows());

    return ResponseObject.response(data);
  }

  public static ResponseObject importClient(String address, String login, String password,
      String companyName, String companyCode, String companyVATCode,
      String companyAddress, String companyPostIndex, String companyCity, String companyCountry) {

    ResponseObject response = getPort(address, login, password);

    if (response.hasErrors()) {
      return response;
    }
    logger.debug("ImportClient:", "importing client...");

    StringBuilder sb = new StringBuilder("<client>")
        .append(tag("klientas", companyName))
        .append(tag("kodas", companyCode))
        .append(tag("pvm_kodas", companyVATCode))
        .append(tag("adresas", companyAddress))
        .append(tag("indeksas", companyPostIndex))
        .append(tag("miestas", companyCity))
        .append(tag("salis", companyCountry))
        .append("</client>");

    String answer = ((ButentWebServiceSoapPort) response.getResponse())
        .process("ImportClient", sb.toString());

    try {
      Node node = XmlUtils.fromString(answer).getFirstChild();

      if (BeeUtils.same(node.getLocalName(), "Error")) {
        return ResponseObject.error(node.getTextContent());
      } else {
        answer = XmlUtils.getElements(answer, null).get("klientas");
      }
    } catch (Exception e) {
      return ResponseObject.error(answer);
    }
    logger.debug("ImportClient:", "import succeeded. ClientName =", answer);

    return ResponseObject.response(answer);
  }

  public static ResponseObject importDoc(String address, String login, String password,
      WSDocument doc) {

    ResponseObject response = getPort(address, login, password);

    if (response.hasErrors()) {
      return response;
    }
    logger.debug("ImportDoc:", "importing document...");

    String answer = ((ButentWebServiceSoapPort) response.getResponse())
        .process("ImportDoc", doc.getXml());

    if (!BeeUtils.same(answer, "OK")) {
      try {
        Node node = XmlUtils.fromString(answer).getFirstChild();

        if (BeeUtils.same(node.getLocalName(), "Error")) {
          return ResponseObject.error(node.getTextContent());
        }
      } catch (Exception e) {
        return ResponseObject.error(answer);
      }
    } else {
      logger.debug("ImportDoc:", "import succeeded");
    }
    return ResponseObject.response(answer);
  }

  public static ResponseObject importItem(String address, String login, String password,
      String itemName, String brandName, String brandCode) {

    ResponseObject response = getPort(address, login, password);

    if (response.hasErrors()) {
      return response;
    }
    logger.debug("ImportItem:", "importing item...");

    StringBuilder sb = new StringBuilder("<item>")
        .append(tag("pavad", itemName))
        .append(tag("gamintojas", brandName))
        .append(tag("gam_art", brandCode))
        .append("</item>");

    String answer = ((ButentWebServiceSoapPort) response.getResponse())
        .process("ImportItem", sb.toString());

    try {
      Node node = XmlUtils.fromString(answer).getFirstChild();

      if (BeeUtils.same(node.getLocalName(), "Error")) {
        return ResponseObject.error(node.getTextContent());
      } else {
        answer = node.getTextContent();
      }
    } catch (Exception e) {
      return ResponseObject.error(answer);
    }
    logger.debug("ImportItem:", "import succeeded. New ItemID =", answer);

    return ResponseObject.response(answer);
  }

  public static String tag(String tagName, Object value) {
    if (value == null) {
      return "";
    }
    return new StringBuilder("<").append(tagName).append(">")
        .append(value)
        .append("</").append(tagName).append(">")
        .toString();
  }

  public ButentWS(URL wsdlLocation) {
    super(wsdlLocation, new QName(NAMESPACE, NAME));
  }

  public ButentWS(URL wsdlLocation, QName serviceName) {
    super(wsdlLocation, serviceName);
  }

  /**
   * 
   * @return returns ButentWebServiceSoapPort
   */
  @WebEndpoint(name = PORT)
  public ButentWebServiceSoapPort getButentWebServiceSoapPort() {
    return super.getPort(new QName(NAMESPACE, PORT), ButentWebServiceSoapPort.class);
  }

  /**
   * 
   * @param features A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.
   *          Supported features not in the <code>features</code> parameter will have their default
   *          values.
   * @return returns ButentWebServiceSoapPort
   */
  @WebEndpoint(name = PORT)
  public ButentWebServiceSoapPort getButentWebServiceSoapPort(WebServiceFeature... features) {
    return super.getPort(new QName(NAMESPACE, PORT), ButentWebServiceSoapPort.class, features);
  }

}