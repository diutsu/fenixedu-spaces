package net.sourceforge.fenixedu.domain.space;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sourceforge.fenixedu.domain.exception.SpaceDomainException;
import net.sourceforge.fenixedu.domain.resource.Resource;
import net.sourceforge.fenixedu.domain.resource.ResourceAllocation;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.groups.Group;
import org.fenixedu.bennu.core.domain.groups.UnionGroup;
import org.fenixedu.commons.StringNormalizer;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.YearMonthDay;

public abstract class Space extends Space_Base {

    public abstract Integer getExamCapacity();

    public abstract Integer getNormalCapacity();

    public final static Comparator<Space> COMPARATOR_BY_PRESENTATION_NAME = new Comparator<Space>() {
        @Override
        public int compare(Space o1, Space o2) {

            if (o1.isFloor() && o2.isFloor()) {
                return compareFloors((Floor) o1, (Floor) o2);
            }

            return comparePresentationName(o1, o2);
        }
    };

    public final static Comparator<Space> COMPARATOR_BY_NAME_FLOOR_BUILDING_AND_CAMPUS = new Comparator<Space>() {
        @Override
        public int compare(Space o1, Space o2) {

            Integer buildingCheck = checkObjects(o1.getSpaceBuilding(), o2.getSpaceBuilding());
            if (buildingCheck != null) {
                return buildingCheck.intValue();
            }

            Integer campusCheck = checkObjects(o1.getSpaceCampus(), o2.getSpaceCampus());
            if (campusCheck != null) {
                return campusCheck.intValue();
            }

            Integer floorCheck = checkObjects(o1.getSpaceFloorWithIntermediary(), o2.getSpaceFloorWithIntermediary());
            if (floorCheck != null) {
                return floorCheck.intValue();
            }

            return comparePresentationName(o1, o2);
        }

        private Integer checkObjects(Space space1, Space space2) {

            if (space1 != null && space2 == null) {
                return Integer.valueOf(1);

            } else if (space1 == null && space2 != null) {
                return Integer.valueOf(-1);

            } else if (space1 == null && space2 == null) {
                return null;

            } else if (!space1.equals(space2)) {
                if (space1.isFloor() && space2.isFloor()) {
                    return compareFloors((Floor) space1, (Floor) space2);
                }
                return comparePresentationName(space1, space2);
            }

            return null;
        }
    };

    private static int comparePresentationName(Space space1, Space space2) {
        int compareTo =
                space1.getSpaceInformation().getPresentationName().compareTo(space2.getSpaceInformation().getPresentationName());
        if (compareTo == 0) {
            return space1.getExternalId().compareTo(space2.getExternalId());
        }
        return compareTo;
    }

    private static int compareFloors(Floor floor1, Floor floor2) {
        int compareTo = floor1.getSpaceInformation().getLevel().compareTo(floor2.getSpaceInformation().getLevel());
        if (compareTo == 0) {
            return floor1.getExternalId().compareTo(floor2.getExternalId());
        }
        return compareTo;
    }

    protected Space() {
        super();
        setCreatedOn(new YearMonthDay());
    }

//    @FenixDomainObjectActionLogAnnotation(actionName = "Set new Parent space", parameters = { "this", "newParentSpace" })
    public void setNewPossibleParentSpace(Space newParentSpace) {
//        check(this, SpacePredicates.checkPermissionsToManageSpace);
        if (newParentSpace != null) {
            setSuroundingSpace(newParentSpace);
        }
    }

    @Override
    public boolean isSpace() {
        return true;
    }

    public SpaceInformation getMostRecentSpaceInformation() {
        SpaceInformation selectedSpaceInformation = null;
        for (final SpaceInformation spaceInformation : getSpaceInformations()) {
            if (spaceInformation.getValidUntil() == null) {
                return spaceInformation;
            } else if (selectedSpaceInformation == null
                    || spaceInformation.getValidUntil().isAfter(selectedSpaceInformation.getValidUntil())) {
                selectedSpaceInformation = spaceInformation;
            }
        }
        return selectedSpaceInformation;
    }

    public SpaceInformation getSpaceInformation() {
        return getSpaceInformation(null);
    }

    public SpaceInformation getSpaceInformation(YearMonthDay when) {
        when = (when == null) ? new YearMonthDay() : when;
        for (final SpaceInformation spaceInformation : getSpaceInformations()) {
            if (spaceInformation.isActive(when)) {
                return spaceInformation;
            }
        }
        return getMostRecentSpaceInformation();
    }

    public SortedSet<SpaceInformation> getOrderedSpaceInformations() {
        return new TreeSet<SpaceInformation>(getSpaceInformations());
    }

    public Blueprint getSuroundingSpaceMostRecentBlueprint() {
        Space suroundingSpace = getSuroundingSpace();
        if (suroundingSpace != null) {
            return suroundingSpace.getMostRecentBlueprint();
        }
        return null;
    }

    public Blueprint getMostRecentBlueprint() {
        SortedSet<Blueprint> orderedBlueprints = getOrderedBlueprints();
        return (!orderedBlueprints.isEmpty()) ? orderedBlueprints.last() : null;
    }

    public SortedSet<Blueprint> getOrderedBlueprints() {
        return new TreeSet<Blueprint>(getBlueprints());
    }

//    public List<WrittenEvaluationSpaceOccupation> getWrittenEvaluationSpaceOccupations() {
//        List<WrittenEvaluationSpaceOccupation> occupations = new ArrayList<WrittenEvaluationSpaceOccupation>();
//        for (ResourceAllocation allocation : getResourceAllocations()) {
//            if (allocation.isWrittenEvaluationSpaceOccupation()) {
//                occupations.add((WrittenEvaluationSpaceOccupation) allocation);
//            }
//        }
//        return occupations;
//    }

//    public List<PersonSpaceOccupation> getPersonSpaceOccupations() {
//        List<PersonSpaceOccupation> personSpaceOccupations = new ArrayList<PersonSpaceOccupation>();
//        for (ResourceAllocation allocation : getResourceAllocations()) {
//            if (allocation.isPersonSpaceOccupation()) {
//                personSpaceOccupations.add((PersonSpaceOccupation) allocation);
//            }
//        }
//        return personSpaceOccupations;
//    }

//    public int getSpaceResponsibilityCount() {
//        return getSpaceResponsibility().size();
//    }

//    public List<SpaceResponsibility> getSpaceResponsibility() {
//        List<SpaceResponsibility> result = new ArrayList<SpaceResponsibility>();
//        for (ResourceResponsibility responsibility : getResourceResponsibility()) {
//            if (responsibility.isSpaceResponsibility()) {
//                result.add((SpaceResponsibility) responsibility);
//            }
//        }
//        return result;
//    }

//    public List<MaterialSpaceOccupation> getMaterialSpaceOccupations() {
//        List<MaterialSpaceOccupation> materialSpaceOccupations = new ArrayList<MaterialSpaceOccupation>();
//        for (ResourceAllocation allocation : getResourceAllocations()) {
//            if (allocation.isMaterialSpaceOccupation()) {
//                materialSpaceOccupations.add((MaterialSpaceOccupation) allocation);
//            }
//        }
//        return materialSpaceOccupations;
//    }

//    public List<UnitSpaceOccupation> getUnitSpaceOccupations() {
//        List<UnitSpaceOccupation> unitSpaceOccupations = new ArrayList<UnitSpaceOccupation>();
//        for (ResourceAllocation allocation : getResourceAllocations()) {
//            if (allocation.isUnitSpaceOccupation()) {
//                unitSpaceOccupations.add((UnitSpaceOccupation) allocation);
//            }
//        }
//        return unitSpaceOccupations;
//    }

    public Map<EventSpaceOccupation, List<Interval>> getEventSpaceOccupations(DateTime start, DateTime end) {
        Map<EventSpaceOccupation, List<Interval>> occupationIntervals = new HashMap<EventSpaceOccupation, List<Interval>>();
        for (ResourceAllocation occupation : getResourceAllocations()) {
            if (occupation.isEventSpaceOccupation()) {
                EventSpaceOccupation spaceOccupation = (EventSpaceOccupation) occupation;
                final List<Interval> intervals = spaceOccupation.getEventSpaceOccupationIntervals(start, end);
                if (!intervals.isEmpty()) {
                    occupationIntervals.put(spaceOccupation, intervals);
                }
            }
        }
        return occupationIntervals;
    }

    public Set<AllocatableSpace> getAllActiveContainedAllocatableSpaces() {
        Set<AllocatableSpace> spaces = new HashSet<AllocatableSpace>();
        for (Space space : getContainedSpaces()) {
            if (space.isActive()) {
                if (!space.isAllocatableSpace()) {
                    spaces.addAll(space.getAllActiveContainedAllocatableSpaces());
                } else {
                    spaces.add((AllocatableSpace) space);
                }
            }
        }
        return spaces;
    }

    public Set<? extends Space> getActiveContainedSpaces() {
        Set<Space> result = new TreeSet<Space>(Space.COMPARATOR_BY_PRESENTATION_NAME);
        for (Space space : getContainedSpaces()) {
            if (space.getSpaceInformation() != null && space.isActive()) {
                result.add(space);
            }
        }
        return result;
    }

    public int getActiveContainedSpacesCount() {
        return getActiveContainedSpaces().size();
    }

    public Set<? extends Space> getActiveContainedSpacesByType(Class<? extends Space> clazz) {
        Set<Space> result = new TreeSet<Space>(Space.COMPARATOR_BY_PRESENTATION_NAME);
        for (Space space : getContainedSpaces()) {
            if (space.getClass().equals(clazz) && space.isActive()) {
                result.add(space);
            }
        }
        return result;
    }

    public Set<Space> getContainedSpacesByState(SpaceState spaceState) {
        Set<Space> result = new TreeSet<Space>(Space.COMPARATOR_BY_PRESENTATION_NAME);
        for (Space space : getContainedSpaces()) {
            if ((spaceState.equals(SpaceState.ACTIVE) && space.isActive()) || spaceState.equals(SpaceState.INACTIVE)
                    && !space.isActive()) {
                result.add(space);
            }
        }
        return result;
    }

//    public SortedSet<PersonSpaceOccupation> getActivePersonSpaceOccupations() {
//        return getPersonSpaceOccupationsByState(true);
//    }
//
//    public SortedSet<PersonSpaceOccupation> getInactivePersonSpaceOccupations() {
//        return getPersonSpaceOccupationsByState(false);
//    }

//    private SortedSet<PersonSpaceOccupation> getPersonSpaceOccupationsByState(boolean state) {
//        SortedSet<PersonSpaceOccupation> personSpaceOccupations =
//                new TreeSet<PersonSpaceOccupation>(PersonSpaceOccupation.COMPARATOR_BY_PERSON_NAME_AND_OCCUPATION_INTERVAL);
//        YearMonthDay current = new YearMonthDay();
//        for (PersonSpaceOccupation personSpaceOccupation : getPersonSpaceOccupations()) {
//            if (personSpaceOccupation.contains(current) == state) {
//                personSpaceOccupations.add(personSpaceOccupation);
//            }
//        }
//        return personSpaceOccupations;
//    }

//    public SortedSet<SpaceResponsibility> getActiveSpaceResponsibility() {
//        return getSpaceResponsabilityByState(true);
//    }
//
//    public SortedSet<SpaceResponsibility> getInactiveSpaceResponsibility() {
//        return getSpaceResponsabilityByState(false);
//    }
//
//    private SortedSet<SpaceResponsibility> getSpaceResponsabilityByState(boolean state) {
//        SortedSet<SpaceResponsibility> spaceResponsibility =
//                new TreeSet<SpaceResponsibility>(SpaceResponsibility.COMPARATOR_BY_UNIT_NAME_AND_RESPONSIBILITY_INTERVAL);
//        YearMonthDay current = new YearMonthDay();
//        for (ResourceResponsibility responsibility : getResourceResponsibilitySet()) {
//            if (responsibility.isSpaceResponsibility() && responsibility.isActive(current) == state) {
//                spaceResponsibility.add((SpaceResponsibility) responsibility);
//            }
//        }
//        return spaceResponsibility;
//    }
//
//    public SortedSet<UnitSpaceOccupation> getActiveUnitSpaceOccupations() {
//        return getUnitSpaceOccupationsByState(true);
//    }
//
//    public SortedSet<UnitSpaceOccupation> getInactiveUnitSpaceOccupations() {
//        return getUnitSpaceOccupationsByState(false);
//    }
//
//    private SortedSet<UnitSpaceOccupation> getUnitSpaceOccupationsByState(boolean state) {
//        SortedSet<UnitSpaceOccupation> unitSpaceOccupations =
//                new TreeSet<UnitSpaceOccupation>(UnitSpaceOccupation.COMPARATOR_BY_OCCUPATION_INTERVAL_AND_UNIT);
//        YearMonthDay current = new YearMonthDay();
//        for (UnitSpaceOccupation unitSpaceOccupation : getUnitSpaceOccupations()) {
//            if (unitSpaceOccupation.isActive(current) == state) {
//                unitSpaceOccupations.add(unitSpaceOccupation);
//            }
//        }
//        return unitSpaceOccupations;
//    }
//
//    public SortedSet<Material> getActiveSpaceMaterial() {
//        SortedSet<Material> spaceMaterial = new TreeSet<Material>(Material.COMPARATOR_BY_CLASS_NAME);
//        YearMonthDay current = new YearMonthDay();
//        for (MaterialSpaceOccupation materialSpaceOccupation : getMaterialSpaceOccupations()) {
//            if (materialSpaceOccupation.isActive(current)) {
//                spaceMaterial.add(materialSpaceOccupation.getMaterial());
//            }
//        }
//        return spaceMaterial;
//    }
//
//    public SortedSet<MaterialSpaceOccupation> getActiveMaterialSpaceOccupationsToLoggedPerson() {
//        return getMaterialSpaceOccupationsToLoggedPersonByState(true);
//    }
//
//    public SortedSet<MaterialSpaceOccupation> getInactiveMaterialSpaceOccupationsToLoggedPerson() {
//        return getMaterialSpaceOccupationsToLoggedPersonByState(false);
//    }
//
//    private SortedSet<MaterialSpaceOccupation> getMaterialSpaceOccupationsToLoggedPersonByState(boolean state) {
//
//        SortedSet<MaterialSpaceOccupation> materialOccupations =
//                new TreeSet<MaterialSpaceOccupation>(MaterialSpaceOccupation.COMPARATOR_BY_CLASS_NAME);
//        YearMonthDay current = new YearMonthDay();
//        User loggedPerson = Authenticate.getUser();
//
//        for (MaterialSpaceOccupation materialSpaceOccupation : getMaterialSpaceOccupations()) {
//            if (materialSpaceOccupation.isActive(current) == state
//                    && (materialSpaceOccupation.getSpace().personHasPermissionsToManageSpace(loggedPerson) || (materialSpaceOccupation
//                            .getAccessGroup() != null && materialSpaceOccupation.getAccessGroup().isMember(loggedPerson)))) {
//                materialOccupations.add(materialSpaceOccupation);
//            }
//        }
//        return materialOccupations;
//    }
//
//    public Set<? extends MaterialSpaceOccupation> getMaterialSpaceOccupationsByMaterialClass(
//            Class<? extends MaterialSpaceOccupation> clazz) {
//        Set<MaterialSpaceOccupation> materialOccupations = new HashSet<MaterialSpaceOccupation>();
//        for (MaterialSpaceOccupation occupation : getMaterialSpaceOccupations()) {
//            if (occupation.getClass().equals(clazz)) {
//                materialOccupations.add(occupation);
//            }
//        }
//        return materialOccupations;
//    }

//    public static Set<DomainObjectActionLog> getListOfChangesInSpacesOrderedByInstant() {
//        Set<Class<? extends DomainObject>> classs = new HashSet<Class<? extends DomainObject>>();
//        User loggedPerson = Authenticate.getUser();
//        if (personIsSpacesAdministrator(loggedPerson)) {
//            classs.add(Room.class);
//            classs.add(Floor.class);
//            classs.add(Campus.class);
//            classs.add(Building.class);
//            classs.add(Blueprint.class);
//            classs.add(RoomSubdivision.class);
//            classs.add(RoomInformation.class);
//            classs.add(FloorInformation.class);
//            classs.add(CampusInformation.class);
//            classs.add(RoomClassification.class);
//            classs.add(BuildingInformation.class);
//            classs.add(UnitSpaceOccupation.class);
//            classs.add(SpaceResponsibility.class);
//            classs.add(PersonSpaceOccupation.class);
//            classs.add(ExtensionSpaceOccupation.class);
//            classs.add(RoomSubdivisionInformation.class);
//            return DomainObjectActionLog.readDomainObjectActionLogsOrderedByInstant(classs);
//        }
//        return new HashSet<DomainObjectActionLog>();
//    }

    public Space readSubSpaceByBlueprintNumber(String blueprintNumber) {
        if (blueprintNumber != null && !StringUtils.isEmpty(blueprintNumber)) {
            for (Space space : getContainedSpaces()) {
                if (space.getSpaceInformation() != null) {
                    String spaceBlueprint = space.getSpaceInformation().getBlueprintNumber();
                    if (spaceBlueprint != null && !StringUtils.isEmpty(spaceBlueprint) && spaceBlueprint.equals(blueprintNumber)) {
                        return space;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void delete() {
        if (!canBeDeleted()) {
            throw new SpaceDomainException("error.space.cannot.be.deleted");
        }

        for (; !getBlueprints().isEmpty(); getBlueprints().iterator().next().delete()) {
            ;
        }
        for (; !getSpaceInformations().isEmpty(); getSpaceInformations().iterator().next()
                .deleteWithoutCheckNumberOfSpaceInformations()) {
            ;
        }
//        for (SpaceAttendances attendance : getPastAttendancesSet()) {
//            attendance.delete();
//        }
//
//        for (SpaceAttendances attendance : getCurrentAttendanceSet()) {
//            attendance.delete();
//        }

        super.setSuroundingSpace(null);
        super.delete();
    }

    private boolean canBeDeleted() {
        return !hasAnyContainedSpaces();
    }

    public boolean isActive() {
        // System.out.println("SPACE INFO: " + getMostRecentSpaceInformation() +
        // " for " + new YearMonthDay());
        final SpaceInformation mostRecentSpaceInformation = getMostRecentSpaceInformation();
        return mostRecentSpaceInformation != null && mostRecentSpaceInformation.isActive(new YearMonthDay());
    }

    public Boolean getActiveFlag() {
        return Boolean.valueOf(isActive());
    }

    public static List<Space> getAllSpacesByPresentationName(String name) {
        List<Space> result = new ArrayList<Space>();
        String[] identificationWords = getIdentificationWords(name);
        for (Resource resource : Bennu.getInstance().getResourcesSet()) {
            if (resource.isSpace() && ((Space) resource).verifyNameEquality(identificationWords)) {
                result.add((Space) resource);
            }
        }
        return result;
    }

    protected boolean verifyNameEquality(String[] nameWords) {
        if (nameWords != null) {
            String spacePresentationName = getSpaceInformation().getPresentationName();
            if (spacePresentationName != null) {
                String[] spaceIdentificationWords = StringNormalizer.normalize(spacePresentationName).trim().split(" ");
                int j, i;
                for (i = 0; i < nameWords.length; i++) {
                    if (!nameWords[i].equals("")) {
                        for (j = 0; j < spaceIdentificationWords.length; j++) {
                            if (spaceIdentificationWords[j].equals(nameWords[i])) {
                                break;
                            }
                        }
                        if (j == spaceIdentificationWords.length) {
                            return false;
                        }
                    }
                }
                if (i == nameWords.length) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String[] getIdentificationWords(String name) {
        String[] identificationWords = null;
        if (name != null && !StringUtils.isEmpty(name.trim())) {
            identificationWords = StringNormalizer.normalize(name).trim().split(" ");
        }
        return identificationWords;
    }

    public static List<Campus> getAllCampus() {
        return (List<Campus>) getAllSpacesByClass(Campus.class, null);
    }

    public static List<Campus> getAllActiveCampus() {
        return (List<Campus>) getAllSpacesByClass(Campus.class, Boolean.TRUE);
    }

    public static List<Building> getAllActiveBuildings() {
        return (List<Building>) getAllSpacesByClass(Building.class, Boolean.TRUE);
    }

    public static List<Building> getAllActiveBuildingsOrderedByName() {
        final List<Building> allActiveBuildings = getAllActiveBuildings();
        Collections.sort(allActiveBuildings, Building.COMPARATOR_BUILDING_BY_NAME);
        return allActiveBuildings;
    }

    private static List<? extends Space> getAllSpacesByClass(Class<? extends Space> clazz, Boolean active) {
        List<Space> result = new ArrayList<Space>();
        for (Resource space : Bennu.getInstance().getResourcesSet()) {
            if (space.getClass().equals(clazz) && (active == null || ((Space) space).isActive() == active.booleanValue())) {
                result.add((Space) space);
            }
        }
        return result;
    }

    public List<AllocatableSpace> getAllActiveSubRoomsForEducation() {
        List<AllocatableSpace> result = new ArrayList<AllocatableSpace>();
        Collection<Space> containedSpaces = getContainedSpaces();
        for (Space space : containedSpaces) {
            if (space.isAllocatableSpace() && space.isActive() && ((AllocatableSpace) space).isForEducation()) {
                result.add((AllocatableSpace) space);
            }
        }
        for (Space subSpace : containedSpaces) {
            result.addAll(subSpace.getAllActiveSubRoomsForEducation());
        }
        return result;
    }

    public static boolean personIsSpacesAdministrator(User person) {
//        return (person.hasRole(RoleType.MANAGER) || person.hasRole(RoleType.SPACE_MANAGER_SUPER_USER))
//                && person.hasRole(RoleType.SPACE_MANAGER);

        //TODO: groups
        return false;

    }

    public void checkIfLoggedPersonHasPermissionsToManageSpace(User person) {
        if (personHasPermissionsToManageSpace(person)) {
            return;
        }
        throw new SpaceDomainException("error.logged.person.not.authorized.to.make.operation");
    }

    public void checkIfLoggedPersonIsSpacesAdministrator(User person) {
        if (personIsSpacesAdministrator(person)) {
            return;
        }
        throw new SpaceDomainException("error.logged.person.not.authorized.to.make.operation");
    }

    public boolean personHasPermissionsToManageSpace(User person) {
        return personIsSpacesAdministrator(person) || personHasSpecialPermissionToManageSpace(person);
    }

    public boolean personHasSpecialPermissionToManageSpace(User person) {
        Group accessGroup = getSpaceManagementAccessGroupWithChainOfResponsibility();
        return accessGroup != null && accessGroup.isMember(person);
    }

    public boolean personHasPermissionToManagePersonOccupations(User person) {
        Group accessGroup = getPersonOccupationsAccessGroupWithChainOfResponsibility();
        return accessGroup != null && accessGroup.isMember(person);
    }

    public boolean personHasPermissionToManageExtensionOccupations(User person) {
        Group accessGroup = getExtensionOccupationsAccessGroupWithChainOfResponsibility();
        return accessGroup != null && accessGroup.isMember(person);
    }

    public boolean personHasPermissionToManageUnitOccupations(User person) {
        Group accessGroup = getUnitOccupationsAccessGroupWithChainOfResponsibility();
        return accessGroup != null && accessGroup.isMember(person);
    }

    public boolean personHasPermissionToManageLessonOccupations(User person) {
        Group accessGroup = getLessonOccupationsAccessGroupWithChainOfResponsibility();
        return accessGroup != null && accessGroup.isMember(person);
    }

    public boolean personHasPermissionToManageWrittenEvaluationOccupations(User person) {
        Group accessGroup = getWrittenEvaluationOccupationsAccessGroup();
        return accessGroup != null && accessGroup.isMember(person);
    }

    public boolean personHasPermissionToGenericEventOccupations(User person) {
        Group accessGroup = getGenericEventOccupationsAccessGroup();
        return accessGroup != null && accessGroup.isMember(person);
    }

    public Group getPersonOccupationsAccessGroupWithChainOfResponsibility() {
        final Group accessGroup = getPersonOccupationsAccessGroup();
        if (accessGroup != null && !accessGroup.getMembers().isEmpty()) {
            return accessGroup;
        }
        final Space surroundingSpace = getSuroundingSpace();
        if (surroundingSpace != null) {
            return surroundingSpace.getPersonOccupationsAccessGroupWithChainOfResponsibility();
        }
        return null;
    }

    public Group getUnitOccupationsAccessGroupWithChainOfResponsibility() {
        final Group accessGroup = getUnitOccupationsAccessGroup();
        if (accessGroup != null && !accessGroup.getMembers().isEmpty()) {
            return accessGroup;
        }
        final Space surroundingSpace = getSuroundingSpace();
        if (surroundingSpace != null) {
            return surroundingSpace.getUnitOccupationsAccessGroupWithChainOfResponsibility();
        }
        return null;
    }

    public Group getExtensionOccupationsAccessGroupWithChainOfResponsibility() {
        final Group accessGroup = getExtensionOccupationsAccessGroup();
        if (accessGroup != null && !accessGroup.getMembers().isEmpty()) {
            return accessGroup;
        }
        final Space surroundingSpace = getSuroundingSpace();
        if (surroundingSpace != null) {
            return surroundingSpace.getExtensionOccupationsAccessGroupWithChainOfResponsibility();
        }
        return null;
    }

    public Group getSpaceManagementAccessGroupWithChainOfResponsibility() {
        final Space space = getSpaceWithChainOfResponsibility();
        return space == null ? null : space.getSpaceManagementAccessGroup();
    }

    public Space getSpaceWithChainOfResponsibility() {
        final Group accessGroup = getSpaceManagementAccessGroup();
        if (accessGroup != null && !accessGroup.getMembers().isEmpty()) {
            return this;
        }
        final Space surroundingSpace = getSuroundingSpace();
        if (surroundingSpace != null) {
            return surroundingSpace.getSpaceWithChainOfResponsibility();
        }
        return null;
    }

    public Group getLessonOccupationsAccessGroupWithChainOfResponsibility() {
        final Group thisGroup = getLessonOccupationsAccessGroup();
        if (thisGroup != null && !thisGroup.getMembers().isEmpty()) {
            return thisGroup;
        }
        final Space surroundingSpace = getSuroundingSpace();
        if (surroundingSpace != null) {
            return surroundingSpace.getLessonOccupationsAccessGroupWithChainOfResponsibility();
        }
        return null;
    }

    public Group getWrittenEvaluationOccupationsAccessGroupWithChainOfResponsibility() {
        final Group thisGroup = getWrittenEvaluationOccupationsAccessGroup();
        if (thisGroup != null && !thisGroup.getMembers().isEmpty()) {
            return thisGroup;
        }
        final Space surroundingSpace = getSuroundingSpace();
        if (surroundingSpace != null) {
            return surroundingSpace.getWrittenEvaluationOccupationsAccessGroupWithChainOfResponsibility();
        }
        return null;
    }

    public Group getGenericEventOccupationsAccessGroupWithChainOfResponsibility() {
        final Group thisGroup = getGenericEventOccupationsAccessGroup();
        if (thisGroup != null && !thisGroup.getMembers().isEmpty()) {
            return thisGroup;
        }
        final Space surroundingSpace = getSuroundingSpace();
        if (surroundingSpace != null) {
            return surroundingSpace.getGenericEventOccupationsAccessGroupWithChainOfResponsibility();
        }
        return null;
    }

//    @FenixDomainObjectActionLogAnnotation(actionName = "Add or remove person from access group", parameters = { "this",
//            "accessGroupType", "toAdd", "isToMaintainElements", "expression" })
    public void addOrRemovePersonFromAccessGroup(SpaceAccessGroupType accessGroupType, Boolean toAdd,
            Boolean isToMaintainElements, String expression) throws DomainException {
//        check(this, SpacePredicates.checkIfLoggedPersonIsSpaceAdministrator);

        if (StringUtils.isEmpty(expression)) {
            throw new SpaceDomainException("error.space.access.groups.management.no.person");
        }

        if (!toAdd) {
            byte[] encodeHex;
            try {
                encodeHex = Hex.decodeHex(expression.toCharArray());
            } catch (DecoderException e) {
                throw new SpaceDomainException("error.space.access.groups.invalid.expression");
            }
            expression = new String(encodeHex);
        }

        Group groupToAddOrRemove = Group.parse(expression);

        Set<User> elementsToAddOrRemove = null;
        Group existentGroup = null, newGroupUnion = null;

        switch (accessGroupType) {

        case PERSON_OCCUPATION_ACCESS_GROUP:

            elementsToAddOrRemove = groupToAddOrRemove.getMembers();
            checkIfPersonAlreadyHasPermissions(elementsToAddOrRemove, toAdd);

            if (isToMaintainElements) {
                existentGroup = getPersonOccupationsAccessGroupWithChainOfResponsibility();
            } else {
                existentGroup = getPersonOccupationsAccessGroup();
            }

            newGroupUnion = manageGroups(toAdd, groupToAddOrRemove, existentGroup);
            setPersonOccupationsAccessGroup(newGroupUnion);

            spaceManagerRoleManagement(elementsToAddOrRemove, toAdd);
            break;

        case EXTENSION_OCCUPATION_ACCESS_GROUP:

            elementsToAddOrRemove = groupToAddOrRemove.getMembers();
            checkIfPersonAlreadyHasPermissions(elementsToAddOrRemove, toAdd);

            if (isToMaintainElements) {
                existentGroup = getExtensionOccupationsAccessGroupWithChainOfResponsibility();
            } else {
                existentGroup = getExtensionOccupationsAccessGroup();
            }

            newGroupUnion = manageGroups(toAdd, groupToAddOrRemove, existentGroup);
            setExtensionOccupationsAccessGroup(newGroupUnion);

            spaceManagerRoleManagement(elementsToAddOrRemove, toAdd);
            break;

        case UNIT_OCCUPATION_ACCESS_GROUP:

            elementsToAddOrRemove = groupToAddOrRemove.getMembers();
            checkIfPersonAlreadyHasPermissions(elementsToAddOrRemove, toAdd);

            if (isToMaintainElements) {
                existentGroup = getUnitOccupationsAccessGroupWithChainOfResponsibility();
            } else {
                existentGroup = getUnitOccupationsAccessGroup();
            }

            newGroupUnion = manageGroups(toAdd, groupToAddOrRemove, existentGroup);
            setUnitOccupationsAccessGroup(newGroupUnion);

            spaceManagerRoleManagement(elementsToAddOrRemove, toAdd);
            break;

        case SPACE_MANAGEMENT_ACCESS_GROUP:

            elementsToAddOrRemove = groupToAddOrRemove.getMembers();
            checkIfPersonAlreadyHasPermissions(elementsToAddOrRemove, toAdd);

            if (isToMaintainElements) {
                existentGroup = getSpaceManagementAccessGroupWithChainOfResponsibility();
            } else {
                existentGroup = getSpaceManagementAccessGroup();
            }

            newGroupUnion = manageGroups(toAdd, groupToAddOrRemove, existentGroup);
            setSpaceManagementAccessGroup(newGroupUnion);

            spaceManagerRoleManagement(elementsToAddOrRemove, toAdd);
            break;

        case LESSON_OCCUPATION_ACCESS_GROUP:

            if (isToMaintainElements) {
                existentGroup = getLessonOccupationsAccessGroupWithChainOfResponsibility();
            } else {
                existentGroup = getLessonOccupationsAccessGroup();
            }

            newGroupUnion = manageGroups(toAdd, groupToAddOrRemove, existentGroup);
            setLessonOccupationsAccessGroup(newGroupUnion);
            break;

        case WRITTEN_EVALUATION_OCCUPATION_ACCESS_GROUP:

            if (isToMaintainElements) {
                existentGroup = getWrittenEvaluationOccupationsAccessGroupWithChainOfResponsibility();
            } else {
                existentGroup = getWrittenEvaluationOccupationsAccessGroup();
            }

            newGroupUnion = manageGroups(toAdd, groupToAddOrRemove, existentGroup);
            setWrittenEvaluationOccupationsAccessGroup(newGroupUnion);
            break;

        case GENERIC_EVENT_SPACE_OCCUPATION_ACCESS_GROUP:

            if (isToMaintainElements) {
                existentGroup = getGenericEventOccupationsAccessGroupWithChainOfResponsibility();
            } else {
                existentGroup = getGenericEventOccupationsAccessGroup();
            }

            newGroupUnion = manageGroups(toAdd, groupToAddOrRemove, existentGroup);
            setGenericEventOccupationsAccessGroup(newGroupUnion);
            break;

        default:
            break;
        }
    }

    //TODO: review this
    private Group manageGroups(Boolean toAdd, Group groupToAddOrRemove, Group existentGroup) {

        Set<Group> existentGroups = new HashSet<>();
        if (existentGroup != null) {
            if (existentGroup instanceof UnionGroup) {
//                existentGroups.addAll(((UnionGroup) existentGroup).getChildrenSet());
            } else {
                existentGroups.add(existentGroup);
            }
        }

        if (toAdd) {
            for (Group existentGroup_ : existentGroups) {
                if (existentGroup_.getMembers().containsAll(groupToAddOrRemove.getMembers())) {
                    toAdd = false;
                    break;
                }
            }
            if (toAdd) {
                existentGroups.add(groupToAddOrRemove);
            }

        } else {
            for (Iterator<Group> iter = existentGroups.iterator(); iter.hasNext();) {
                Group existentGroup_ = iter.next();
                if (existentGroup_.getMembers().size() == groupToAddOrRemove.getMembers().size()
                        && existentGroup_.getMembers().containsAll(groupToAddOrRemove.getMembers())) {
                    iter.remove();
                    existentGroups.remove(existentGroup_);
                }
            }
        }
        return (existentGroups.isEmpty() ? null : existentGroups.size() == 1 ? existentGroups.iterator().next() : UnionGroup
                .getInstance(existentGroups));
    }

    private void checkIfPersonAlreadyHasPermissions(Set<User> persons, boolean toAdd) throws DomainException {
        if (toAdd) {
            for (User person : persons) {
                if (personHasPermissionsToManageSpace(person)) {
                    throw new SpaceDomainException("error.space.access.groups.management.person.already.have.permission");
                }
            }
        }
    }

    //TODO: perms
    private void spaceManagerRoleManagement(Set<User> elementsToManage, boolean toAdd) {
        if (toAdd) {
            for (User person : elementsToManage) {
//                person.addPersonRoleByRoleType(RoleType.SPACE_MANAGER);
            }
        } else {
            for (Resource resource : Bennu.getInstance().getResourcesSet()) {
                if (resource.isSpace()) {
                    Space space = (Space) resource;
                    for (User person : elementsToManage) {
                        if (!personIsSpacesAdministrator(person)
                                && !space.personHasPermissionToManageExtensionOccupations(person)
                                && !space.personHasPermissionToManagePersonOccupations(person)
                                && !space.personHasPermissionToManageUnitOccupations(person)
                                && !space.personHasSpecialPermissionToManageSpace(person)) {
//                            person.removeRoleByType(RoleType.SPACE_MANAGER);
                        }
                    }
                }
            }
        }
    }

    public static enum SpaceAccessGroupType {

        PERSON_OCCUPATION_ACCESS_GROUP("personOccupationsAccessGroup"),

        UNIT_OCCUPATION_ACCESS_GROUP("unitOccupationsAccessGroup"),

        EXTENSION_OCCUPATION_ACCESS_GROUP("extensionOccupationsAccessGroup"),

        SPACE_MANAGEMENT_ACCESS_GROUP("spaceManagementAccessGroup"),

        LESSON_OCCUPATION_ACCESS_GROUP("lessonOccupationsAccessGroup"),

        WRITTEN_EVALUATION_OCCUPATION_ACCESS_GROUP("writtenEvaluationOccupationsAccessGroup"),

        GENERIC_EVENT_SPACE_OCCUPATION_ACCESS_GROUP("genericEventOccupationsAccessGroup");

        private String spaceAccessGroupSlotName;

        private SpaceAccessGroupType(String spaceAccessGroupSlotName) {
            this.spaceAccessGroupSlotName = spaceAccessGroupSlotName;
        }

        public String getName() {
            return name();
        }

        public String getSpaceAccessGroupSlotName() {
            return spaceAccessGroupSlotName;
        }
    }

    public Building getSpaceBuilding() {
        if (isBuilding()) {
            return (Building) this;
        }
        if (getSuroundingSpace() == null) {
            return null;
        }
        return getSuroundingSpace().getSpaceBuilding();
    }

    public Floor getSpaceFloor() {
        if (isFloor()) {
            if (getSuroundingSpace() == null) {
                return (Floor) this;
            } else if (getSuroundingSpace().isFloor()) {
                return getSuroundingSpace().getSpaceFloor();
            } else {
                return (Floor) this;
            }
        }
        if (getSuroundingSpace() == null) {
            return null;
        }
        return getSuroundingSpace().getSpaceFloor();
    }

    public Floor getSpaceFloorWithIntermediary() {
        if (isFloor()) {
            return (Floor) this;
        }
        if (getSuroundingSpace() == null) {
            return null;
        }
        return getSuroundingSpace().getSpaceFloorWithIntermediary();
    }

    public Campus getSpaceCampus() {
        if (isCampus()) {
            return (Campus) this;
        }
        if (getSuroundingSpace() == null) {
            return null;
        }
        return getSuroundingSpace().getSpaceCampus();
    }

    public List<Space> getSpaceFullPath() {
        List<Space> result = new ArrayList<Space>();
        result.add(this);
        Space suroundingSpace = getSuroundingSpace();
        while (suroundingSpace != null) {
            result.add(0, suroundingSpace);
            suroundingSpace = suroundingSpace.getSuroundingSpace();
        }
        return result;
    }

    public Set<Space> getPossibleParentSpacesToMoveSpaceUp() {
        Set<Space> result = new HashSet<Space>();
        if (!(isCampus())) {
            result = getPossibleParentSpacesToMoveSpaceUp(result);
            result.addAll(Space.getAllCampus());
            if (getSuroundingSpace() != null) {
                result.remove(getSuroundingSpace());
            }
        }
        return result;
    }

    private Set<Space> getPossibleParentSpacesToMoveSpaceUp(Set<Space> result) {
        if (getSuroundingSpace() != null) {
            result.add(getSuroundingSpace());
            getSuroundingSpace().getPossibleParentSpacesToMoveSpaceUp(result);
        }
        return result;
    }

    public List<Space> getPossibleParentSpacesToMoveSpaceDown() {
        List<Space> result = new ArrayList<Space>();
        if (!(isCampus())) {
            if (getSuroundingSpace() != null) {
                result.addAll(getSuroundingSpace().getContainedSpaces());
            }
            result.remove(this);
        }
        return result;
    }

    public String getResourceAllocationsResume() {
        StringBuilder builder = new StringBuilder();
        int eventOccupations = 0, personOccupations = 0, unitOccupations = 0, materialOccupations = 0;
        for (ResourceAllocation resourceAllocation : getResourceAllocations()) {
            if (resourceAllocation.isEventSpaceOccupation()) {
                eventOccupations++;
            } else if (resourceAllocation.isPersonSpaceOccupation()) {
                personOccupations++;
            } else if (resourceAllocation.isUnitSpaceOccupation()) {
                unitOccupations++;
            } else if (resourceAllocation.isMaterialSpaceOccupation()) {
                materialOccupations++;
            }
        }
        builder.append(eventOccupations).append(" (Events)").append(", ");
        builder.append(personOccupations).append(" (Persons)").append(", ");
        builder.append(unitOccupations).append(" (Units)").append(", ");
        builder.append(materialOccupations).append(" (Material)");
        return builder.toString();
    }

//    @Deprecated
//    public java.util.Set<net.sourceforge.fenixedu.domain.space.SpaceAttendances> getCurrentAttendance() {
//        return getCurrentAttendanceSet();
//    }
//
//    @Deprecated
//    public boolean hasAnyCurrentAttendance() {
//        return !getCurrentAttendanceSet().isEmpty();
//    }
//
//    @Deprecated
//    public java.util.Set<net.sourceforge.fenixedu.domain.space.SpaceAttendances> getPastAttendances() {
//        return getPastAttendancesSet();
//    }
//
//    @Deprecated
//    public boolean hasAnyPastAttendances() {
//        return !getPastAttendancesSet().isEmpty();
//    }

    @Deprecated
    public java.util.Set<net.sourceforge.fenixedu.domain.space.SpaceInformation> getSpaceInformations() {
        return getSpaceInformationsSet();
    }

    @Deprecated
    public boolean hasAnySpaceInformations() {
        return !getSpaceInformationsSet().isEmpty();
    }

    @Deprecated
    public java.util.Set<net.sourceforge.fenixedu.domain.space.Blueprint> getBlueprints() {
        return getBlueprintsSet();
    }

    @Deprecated
    public boolean hasAnyBlueprints() {
        return !getBlueprintsSet().isEmpty();
    }

    @Deprecated
    public java.util.Set<net.sourceforge.fenixedu.domain.space.Space> getContainedSpaces() {
        return getContainedSpacesSet();
    }

    @Deprecated
    public boolean hasAnyContainedSpaces() {
        return !getContainedSpacesSet().isEmpty();
    }

    @Deprecated
    public boolean hasSuroundingSpace() {
        return getSuroundingSpace() != null;
    }

    @Deprecated
    public boolean hasSpaceManagementAccessGroup() {
        return getSpaceManagementAccessGroup() != null;
    }

    @Deprecated
    public boolean hasCreatedOn() {
        return getCreatedOn() != null;
    }

    @Deprecated
    public boolean hasPersonOccupationsAccessGroup() {
        return getPersonOccupationsAccessGroup() != null;
    }

    @Deprecated
    public boolean hasLessonOccupationsAccessGroup() {
        return getLessonOccupationsAccessGroup() != null;
    }

    @Deprecated
    public boolean hasExtensionOccupationsAccessGroup() {
        return getExtensionOccupationsAccessGroup() != null;
    }

//    @Deprecated
//    public boolean hasBennuForLibrary() {
//        return getRootDomainObjectForLibrary() != null;
//    }

    @Deprecated
    public boolean hasUnitOccupationsAccessGroup() {
        return getUnitOccupationsAccessGroup() != null;
    }

    @Deprecated
    public boolean hasGenericEventOccupationsAccessGroup() {
        return getGenericEventOccupationsAccessGroup() != null;
    }

    @Deprecated
    public boolean hasWrittenEvaluationOccupationsAccessGroup() {
        return getWrittenEvaluationOccupationsAccessGroup() != null;
    }

}
