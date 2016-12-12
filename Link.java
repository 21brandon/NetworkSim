public class Link {
    private String address;
    private int weight;

    public Link(String address, int weight) {
        this.address = address;
        this.weight = weight;
    }

    public String getAddress() {
        return address;
    }

    public int getWeight() {
        return weight;
    }

    public boolean equals(Object compare) {
        if (compare instanceof Link) {
            Link link = (Link) compare;
            return (link.address.equals(address) && link.weight == weight);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return address.hashCode() + weight;
    }
}
