package pl.motonet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for anonymous complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="numer" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "numer"
})
@XmlRootElement(name = "ZwrocRozmiarOferty")
public class ZwrocRozmiarOferty {

  protected String numer;

  /**
   * Gets the value of the numer property.
   * 
   * @return possible object is {@link String }
   * 
   */
  public String getNumer() {
    return numer;
  }

  /**
   * Sets the value of the numer property.
   * 
   * @param value allowed object is {@link String }
   * 
   */
  public void setNumer(String value) {
    this.numer = value;
  }

}
