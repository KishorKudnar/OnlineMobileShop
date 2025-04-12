package model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer companyId;

    @Column(nullable = false, length = 50)
    private String brandName;

    @Column(nullable = false, length = 50)
    private String color;

    private String bio;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Mobile> mobiles;

    public Company() {}

    public Company(String brandName, String color, String bio) {
        this.brandName = brandName;
        this.color = color;
        this.bio = bio;
    }

    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public List<Mobile> getMobiles() { return mobiles; }
    public void setMobiles(List<Mobile> mobiles) { this.mobiles = mobiles; }
}
