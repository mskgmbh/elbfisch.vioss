package org.jpac.vioss.s7;

public class Iec61131Address {
	
	public  enum AccessMode {UNDEFINED, INPUT, OUTPUT, MEMORY};
	public  enum Type       {UNDEFINED, BIT, BYTE, WORD, DWORD};
	
	protected String     addressSpecifier;
	protected AccessMode accessMode;  
	protected Type       type;        
	protected int        bitAddress;  
	protected int        byteAddress;
	protected int        size;     //size of the data item [byte]
	
	public Iec61131Address() {
		this.accessMode  = accessMode.UNDEFINED;
		this.type        = type.UNDEFINED;
	}

	public Iec61131Address(String addressSpecifier) throws InvalidAddressSpecifierException {
		this();
		this.addressSpecifier = addressSpecifier.trim().toUpperCase();
		computeAddress(this.addressSpecifier);
	}
	
	protected void computeAddress(String as) throws InvalidAddressSpecifierException {
		int i = 0;
		for (; i < as.length() && as.charAt(i) == ' ';i++);//skip blanks
		char ioDirChar = as.charAt(i);
		switch(ioDirChar) {
		case 'M':
			accessMode = AccessMode.MEMORY;
			break;
		default:
			throw new InvalidAddressSpecifierException("IEC 61131 address specifier: Addressed item must be 'memory' ('M'): " + as);			
		}
		i++;
		for (; i < as.length() && as.charAt(i) == ' ';i++);//skip blanks
		char typeChar = as.charAt(i);
		switch(typeChar) {
		case 'X':
			type = Type.BIT;
			break;		
		case 'B':
			type = Type.BYTE;
			break;
		case 'W':
			type = Type.WORD;
			break;
		case 'D':
			type = Type.DWORD;
			break;
		default:
			throw new InvalidAddressSpecifierException("IEC 61131 address specifier: Addressed item must be of type bit ('X'), byte ('B'), word ('W') or dword ('D'): " + as);			
		}
		i++;
		for (; i < as.length() && as.charAt(i) == ' ';i++);//skip blanks
		byteAddress = 0;
		while (i < as.length() && as.charAt(i) >= '0' && as.charAt(i) <= '9') {
			byteAddress = 10 * byteAddress + as.charAt(i) - '0';
			i++;
		}
		for (; i < as.length() && as.charAt(i) == ' ';i++);//skip blanks
		if (type == Type.BIT) {
			if (as.charAt(i) != '.') {
				throw new InvalidAddressSpecifierException("IEC 61131 address must contain a bit address: " + as);
			}
			i++;
			bitAddress = 0;
			if (!(i < as.length() && as.charAt(i) >= '0' && as.charAt(i) <= '9')) {
				throw new InvalidAddressSpecifierException("IEC 61131 address must contain a bit address: " + as);
			}
			bitAddress = as.charAt(i) - '0';
			i++;
			if ((i < as.length() && as.charAt(i) >= '0' && as.charAt(i) <= '9')) {
				bitAddress = 10 * bitAddress + as.charAt(i) - '0';
			}
			if (bitAddress > 7) {
				throw new InvalidAddressSpecifierException("IEC 61131 bit address must be in range of 0..7: " + as);
			}
		} else {
			if (i < as.length() && as.charAt(i) == '.') {
				throw new InvalidAddressSpecifierException("IEC 61131 address must not contain a bit address: " + as);
			}			
		}

		switch(type) {
			case BIT:
				size = 1;
				break;
			case BYTE:
				size = 1;
				break;
			case WORD:
				size = 2;
				break;
			case DWORD:
				size = 4;
				break;
			default:
				//cannot happen
				break;
		}
	}
	
	public AccessMode getAccessMode() {
		return accessMode;
	}
	
	public int getByteAddress() {
		return byteAddress;
	}
	
	public int getBitAddress() {
		return bitAddress;
	}
	
	public int getSize() {
		return this.size;
	}

	public String getAddressSpecifier() {
		return this.addressSpecifier;
	}
	
	public String toString() {
		return getClass().getSimpleName() + "(" + accessMode + ", " + type + ", " + byteAddress + ", " + bitAddress + ")";
	}

    public static void main(String[] args){
        try{          
            Iec61131Address adr =  new Iec61131Address("IW0");              
            System.out.println("IecAddress : " + adr);
            adr =  new Iec61131Address("MW1000");              
            System.out.println("IecAddress : " + adr);
            adr =  new Iec61131Address("MX1000.8");              
            System.out.println("IecAddress : " + adr);
        }
        catch(Error | Exception exc){
            exc.printStackTrace();
        }
     }	
}
