// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: mtms.proto

#ifndef GOOGLE_PROTOBUF_INCLUDED_mtms_2eproto
#define GOOGLE_PROTOBUF_INCLUDED_mtms_2eproto

#include <limits>
#include <string>

#include <google/protobuf/port_def.inc>
#if PROTOBUF_VERSION < 3011000
#error This file was generated by a newer version of protoc which is
#error incompatible with your Protocol Buffer headers. Please update
#error your headers.
#endif
#if 3011002 < PROTOBUF_MIN_PROTOC_VERSION
#error This file was generated by an older version of protoc which is
#error incompatible with your Protocol Buffer headers. Please
#error regenerate this file with a newer version of protoc.
#endif

#include <google/protobuf/port_undef.inc>
#include <google/protobuf/io/coded_stream.h>
#include <google/protobuf/arena.h>
#include <google/protobuf/arenastring.h>
#include <google/protobuf/generated_message_table_driven.h>
#include <google/protobuf/generated_message_util.h>
#include <google/protobuf/inlined_string_field.h>
#include <google/protobuf/metadata.h>
#include <google/protobuf/generated_message_reflection.h>
#include <google/protobuf/message.h>
#include <google/protobuf/repeated_field.h>  // IWYU pragma: export
#include <google/protobuf/extension_set.h>  // IWYU pragma: export
#include <google/protobuf/unknown_field_set.h>
// @@protoc_insertion_point(includes)
#include <google/protobuf/port_def.inc>
#define PROTOBUF_INTERNAL_EXPORT_mtms_2eproto
PROTOBUF_NAMESPACE_OPEN
namespace internal {
class AnyMetadata;
}  // namespace internal
PROTOBUF_NAMESPACE_CLOSE

// Internal implementation detail -- do not use these members.
struct TableStruct_mtms_2eproto {
  static const ::PROTOBUF_NAMESPACE_ID::internal::ParseTableField entries[]
    PROTOBUF_SECTION_VARIABLE(protodesc_cold);
  static const ::PROTOBUF_NAMESPACE_ID::internal::AuxillaryParseTableField aux[]
    PROTOBUF_SECTION_VARIABLE(protodesc_cold);
  static const ::PROTOBUF_NAMESPACE_ID::internal::ParseTable schema[1]
    PROTOBUF_SECTION_VARIABLE(protodesc_cold);
  static const ::PROTOBUF_NAMESPACE_ID::internal::FieldMetadata field_metadata[];
  static const ::PROTOBUF_NAMESPACE_ID::internal::SerializationTable serialization_table[];
  static const ::PROTOBUF_NAMESPACE_ID::uint32 offsets[];
};
extern const ::PROTOBUF_NAMESPACE_ID::internal::DescriptorTable descriptor_table_mtms_2eproto;
namespace FlumaionQL {
class MTM;
class MTMDefaultTypeInternal;
extern MTMDefaultTypeInternal _MTM_default_instance_;
}  // namespace FlumaionQL
PROTOBUF_NAMESPACE_OPEN
template<> ::FlumaionQL::MTM* Arena::CreateMaybeMessage<::FlumaionQL::MTM>(Arena*);
PROTOBUF_NAMESPACE_CLOSE
namespace FlumaionQL {

// ===================================================================

class MTM :
    public ::PROTOBUF_NAMESPACE_ID::Message /* @@protoc_insertion_point(class_definition:FlumaionQL.MTM) */ {
 public:
  MTM();
  virtual ~MTM();

  MTM(const MTM& from);
  MTM(MTM&& from) noexcept
    : MTM() {
    *this = ::std::move(from);
  }

  inline MTM& operator=(const MTM& from) {
    CopyFrom(from);
    return *this;
  }
  inline MTM& operator=(MTM&& from) noexcept {
    if (GetArenaNoVirtual() == from.GetArenaNoVirtual()) {
      if (this != &from) InternalSwap(&from);
    } else {
      CopyFrom(from);
    }
    return *this;
  }

  static const ::PROTOBUF_NAMESPACE_ID::Descriptor* descriptor() {
    return GetDescriptor();
  }
  static const ::PROTOBUF_NAMESPACE_ID::Descriptor* GetDescriptor() {
    return GetMetadataStatic().descriptor;
  }
  static const ::PROTOBUF_NAMESPACE_ID::Reflection* GetReflection() {
    return GetMetadataStatic().reflection;
  }
  static const MTM& default_instance();

  static void InitAsDefaultInstance();  // FOR INTERNAL USE ONLY
  static inline const MTM* internal_default_instance() {
    return reinterpret_cast<const MTM*>(
               &_MTM_default_instance_);
  }
  static constexpr int kIndexInFileMessages =
    0;

  friend void swap(MTM& a, MTM& b) {
    a.Swap(&b);
  }
  inline void Swap(MTM* other) {
    if (other == this) return;
    InternalSwap(other);
  }

  // implements Message ----------------------------------------------

  inline MTM* New() const final {
    return CreateMaybeMessage<MTM>(nullptr);
  }

  MTM* New(::PROTOBUF_NAMESPACE_ID::Arena* arena) const final {
    return CreateMaybeMessage<MTM>(arena);
  }
  void CopyFrom(const ::PROTOBUF_NAMESPACE_ID::Message& from) final;
  void MergeFrom(const ::PROTOBUF_NAMESPACE_ID::Message& from) final;
  void CopyFrom(const MTM& from);
  void MergeFrom(const MTM& from);
  PROTOBUF_ATTRIBUTE_REINITIALIZES void Clear() final;
  bool IsInitialized() const final;

  size_t ByteSizeLong() const final;
  const char* _InternalParse(const char* ptr, ::PROTOBUF_NAMESPACE_ID::internal::ParseContext* ctx) final;
  ::PROTOBUF_NAMESPACE_ID::uint8* _InternalSerialize(
      ::PROTOBUF_NAMESPACE_ID::uint8* target, ::PROTOBUF_NAMESPACE_ID::io::EpsCopyOutputStream* stream) const final;
  int GetCachedSize() const final { return _cached_size_.Get(); }

  private:
  inline void SharedCtor();
  inline void SharedDtor();
  void SetCachedSize(int size) const final;
  void InternalSwap(MTM* other);
  friend class ::PROTOBUF_NAMESPACE_ID::internal::AnyMetadata;
  static ::PROTOBUF_NAMESPACE_ID::StringPiece FullMessageName() {
    return "FlumaionQL.MTM";
  }
  private:
  inline ::PROTOBUF_NAMESPACE_ID::Arena* GetArenaNoVirtual() const {
    return nullptr;
  }
  inline void* MaybeArenaPtr() const {
    return nullptr;
  }
  public:

  ::PROTOBUF_NAMESPACE_ID::Metadata GetMetadata() const final;
  private:
  static ::PROTOBUF_NAMESPACE_ID::Metadata GetMetadataStatic() {
    ::PROTOBUF_NAMESPACE_ID::internal::AssignDescriptors(&::descriptor_table_mtms_2eproto);
    return ::descriptor_table_mtms_2eproto.file_level_metadata[kIndexInFileMessages];
  }

  public:

  // nested types ----------------------------------------------------

  // accessors -------------------------------------------------------

  enum : int {
    kFixlegdatesFieldNumber = 3,
    kFixlegamountFieldNumber = 4,
    kFltlegdatesFieldNumber = 5,
    kFltlegamountFieldNumber = 6,
    kDiscountvaluesFieldNumber = 7,
    kLegfractionsFieldNumber = 8,
    kTradeidFieldNumber = 1,
    kCurvenameFieldNumber = 2,
    kErrorFieldNumber = 10,
    kComputetimemicrosFieldNumber = 11,
    kHaserroredFieldNumber = 9,
  };
  // repeated int64 fixlegdates = 3;
  int fixlegdates_size() const;
  private:
  int _internal_fixlegdates_size() const;
  public:
  void clear_fixlegdates();
  private:
  ::PROTOBUF_NAMESPACE_ID::int64 _internal_fixlegdates(int index) const;
  const ::PROTOBUF_NAMESPACE_ID::RepeatedField< ::PROTOBUF_NAMESPACE_ID::int64 >&
      _internal_fixlegdates() const;
  void _internal_add_fixlegdates(::PROTOBUF_NAMESPACE_ID::int64 value);
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< ::PROTOBUF_NAMESPACE_ID::int64 >*
      _internal_mutable_fixlegdates();
  public:
  ::PROTOBUF_NAMESPACE_ID::int64 fixlegdates(int index) const;
  void set_fixlegdates(int index, ::PROTOBUF_NAMESPACE_ID::int64 value);
  void add_fixlegdates(::PROTOBUF_NAMESPACE_ID::int64 value);
  const ::PROTOBUF_NAMESPACE_ID::RepeatedField< ::PROTOBUF_NAMESPACE_ID::int64 >&
      fixlegdates() const;
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< ::PROTOBUF_NAMESPACE_ID::int64 >*
      mutable_fixlegdates();

  // repeated float fixlegamount = 4;
  int fixlegamount_size() const;
  private:
  int _internal_fixlegamount_size() const;
  public:
  void clear_fixlegamount();
  private:
  float _internal_fixlegamount(int index) const;
  const ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >&
      _internal_fixlegamount() const;
  void _internal_add_fixlegamount(float value);
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >*
      _internal_mutable_fixlegamount();
  public:
  float fixlegamount(int index) const;
  void set_fixlegamount(int index, float value);
  void add_fixlegamount(float value);
  const ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >&
      fixlegamount() const;
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >*
      mutable_fixlegamount();

  // repeated int64 fltlegdates = 5;
  int fltlegdates_size() const;
  private:
  int _internal_fltlegdates_size() const;
  public:
  void clear_fltlegdates();
  private:
  ::PROTOBUF_NAMESPACE_ID::int64 _internal_fltlegdates(int index) const;
  const ::PROTOBUF_NAMESPACE_ID::RepeatedField< ::PROTOBUF_NAMESPACE_ID::int64 >&
      _internal_fltlegdates() const;
  void _internal_add_fltlegdates(::PROTOBUF_NAMESPACE_ID::int64 value);
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< ::PROTOBUF_NAMESPACE_ID::int64 >*
      _internal_mutable_fltlegdates();
  public:
  ::PROTOBUF_NAMESPACE_ID::int64 fltlegdates(int index) const;
  void set_fltlegdates(int index, ::PROTOBUF_NAMESPACE_ID::int64 value);
  void add_fltlegdates(::PROTOBUF_NAMESPACE_ID::int64 value);
  const ::PROTOBUF_NAMESPACE_ID::RepeatedField< ::PROTOBUF_NAMESPACE_ID::int64 >&
      fltlegdates() const;
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< ::PROTOBUF_NAMESPACE_ID::int64 >*
      mutable_fltlegdates();

  // repeated float fltlegamount = 6;
  int fltlegamount_size() const;
  private:
  int _internal_fltlegamount_size() const;
  public:
  void clear_fltlegamount();
  private:
  float _internal_fltlegamount(int index) const;
  const ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >&
      _internal_fltlegamount() const;
  void _internal_add_fltlegamount(float value);
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >*
      _internal_mutable_fltlegamount();
  public:
  float fltlegamount(int index) const;
  void set_fltlegamount(int index, float value);
  void add_fltlegamount(float value);
  const ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >&
      fltlegamount() const;
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >*
      mutable_fltlegamount();

  // repeated float discountvalues = 7;
  int discountvalues_size() const;
  private:
  int _internal_discountvalues_size() const;
  public:
  void clear_discountvalues();
  private:
  float _internal_discountvalues(int index) const;
  const ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >&
      _internal_discountvalues() const;
  void _internal_add_discountvalues(float value);
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >*
      _internal_mutable_discountvalues();
  public:
  float discountvalues(int index) const;
  void set_discountvalues(int index, float value);
  void add_discountvalues(float value);
  const ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >&
      discountvalues() const;
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >*
      mutable_discountvalues();

  // repeated float legfractions = 8;
  int legfractions_size() const;
  private:
  int _internal_legfractions_size() const;
  public:
  void clear_legfractions();
  private:
  float _internal_legfractions(int index) const;
  const ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >&
      _internal_legfractions() const;
  void _internal_add_legfractions(float value);
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >*
      _internal_mutable_legfractions();
  public:
  float legfractions(int index) const;
  void set_legfractions(int index, float value);
  void add_legfractions(float value);
  const ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >&
      legfractions() const;
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >*
      mutable_legfractions();

  // string tradeid = 1;
  void clear_tradeid();
  const std::string& tradeid() const;
  void set_tradeid(const std::string& value);
  void set_tradeid(std::string&& value);
  void set_tradeid(const char* value);
  void set_tradeid(const char* value, size_t size);
  std::string* mutable_tradeid();
  std::string* release_tradeid();
  void set_allocated_tradeid(std::string* tradeid);
  private:
  const std::string& _internal_tradeid() const;
  void _internal_set_tradeid(const std::string& value);
  std::string* _internal_mutable_tradeid();
  public:

  // string curvename = 2;
  void clear_curvename();
  const std::string& curvename() const;
  void set_curvename(const std::string& value);
  void set_curvename(std::string&& value);
  void set_curvename(const char* value);
  void set_curvename(const char* value, size_t size);
  std::string* mutable_curvename();
  std::string* release_curvename();
  void set_allocated_curvename(std::string* curvename);
  private:
  const std::string& _internal_curvename() const;
  void _internal_set_curvename(const std::string& value);
  std::string* _internal_mutable_curvename();
  public:

  // string error = 10;
  void clear_error();
  const std::string& error() const;
  void set_error(const std::string& value);
  void set_error(std::string&& value);
  void set_error(const char* value);
  void set_error(const char* value, size_t size);
  std::string* mutable_error();
  std::string* release_error();
  void set_allocated_error(std::string* error);
  private:
  const std::string& _internal_error() const;
  void _internal_set_error(const std::string& value);
  std::string* _internal_mutable_error();
  public:

  // int64 computetimemicros = 11;
  void clear_computetimemicros();
  ::PROTOBUF_NAMESPACE_ID::int64 computetimemicros() const;
  void set_computetimemicros(::PROTOBUF_NAMESPACE_ID::int64 value);
  private:
  ::PROTOBUF_NAMESPACE_ID::int64 _internal_computetimemicros() const;
  void _internal_set_computetimemicros(::PROTOBUF_NAMESPACE_ID::int64 value);
  public:

  // bool haserrored = 9;
  void clear_haserrored();
  bool haserrored() const;
  void set_haserrored(bool value);
  private:
  bool _internal_haserrored() const;
  void _internal_set_haserrored(bool value);
  public:

  // @@protoc_insertion_point(class_scope:FlumaionQL.MTM)
 private:
  class _Internal;

  ::PROTOBUF_NAMESPACE_ID::internal::InternalMetadataWithArena _internal_metadata_;
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< ::PROTOBUF_NAMESPACE_ID::int64 > fixlegdates_;
  mutable std::atomic<int> _fixlegdates_cached_byte_size_;
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< float > fixlegamount_;
  mutable std::atomic<int> _fixlegamount_cached_byte_size_;
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< ::PROTOBUF_NAMESPACE_ID::int64 > fltlegdates_;
  mutable std::atomic<int> _fltlegdates_cached_byte_size_;
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< float > fltlegamount_;
  mutable std::atomic<int> _fltlegamount_cached_byte_size_;
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< float > discountvalues_;
  mutable std::atomic<int> _discountvalues_cached_byte_size_;
  ::PROTOBUF_NAMESPACE_ID::RepeatedField< float > legfractions_;
  mutable std::atomic<int> _legfractions_cached_byte_size_;
  ::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr tradeid_;
  ::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr curvename_;
  ::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr error_;
  ::PROTOBUF_NAMESPACE_ID::int64 computetimemicros_;
  bool haserrored_;
  mutable ::PROTOBUF_NAMESPACE_ID::internal::CachedSize _cached_size_;
  friend struct ::TableStruct_mtms_2eproto;
};
// ===================================================================


// ===================================================================

#ifdef __GNUC__
  #pragma GCC diagnostic push
  #pragma GCC diagnostic ignored "-Wstrict-aliasing"
#endif  // __GNUC__
// MTM

// string tradeid = 1;
inline void MTM::clear_tradeid() {
  tradeid_.ClearToEmptyNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
}
inline const std::string& MTM::tradeid() const {
  // @@protoc_insertion_point(field_get:FlumaionQL.MTM.tradeid)
  return _internal_tradeid();
}
inline void MTM::set_tradeid(const std::string& value) {
  _internal_set_tradeid(value);
  // @@protoc_insertion_point(field_set:FlumaionQL.MTM.tradeid)
}
inline std::string* MTM::mutable_tradeid() {
  // @@protoc_insertion_point(field_mutable:FlumaionQL.MTM.tradeid)
  return _internal_mutable_tradeid();
}
inline const std::string& MTM::_internal_tradeid() const {
  return tradeid_.GetNoArena();
}
inline void MTM::_internal_set_tradeid(const std::string& value) {
  
  tradeid_.SetNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), value);
}
inline void MTM::set_tradeid(std::string&& value) {
  
  tradeid_.SetNoArena(
    &::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::move(value));
  // @@protoc_insertion_point(field_set_rvalue:FlumaionQL.MTM.tradeid)
}
inline void MTM::set_tradeid(const char* value) {
  GOOGLE_DCHECK(value != nullptr);
  
  tradeid_.SetNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::string(value));
  // @@protoc_insertion_point(field_set_char:FlumaionQL.MTM.tradeid)
}
inline void MTM::set_tradeid(const char* value, size_t size) {
  
  tradeid_.SetNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      ::std::string(reinterpret_cast<const char*>(value), size));
  // @@protoc_insertion_point(field_set_pointer:FlumaionQL.MTM.tradeid)
}
inline std::string* MTM::_internal_mutable_tradeid() {
  
  return tradeid_.MutableNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
}
inline std::string* MTM::release_tradeid() {
  // @@protoc_insertion_point(field_release:FlumaionQL.MTM.tradeid)
  
  return tradeid_.ReleaseNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
}
inline void MTM::set_allocated_tradeid(std::string* tradeid) {
  if (tradeid != nullptr) {
    
  } else {
    
  }
  tradeid_.SetAllocatedNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), tradeid);
  // @@protoc_insertion_point(field_set_allocated:FlumaionQL.MTM.tradeid)
}

// string curvename = 2;
inline void MTM::clear_curvename() {
  curvename_.ClearToEmptyNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
}
inline const std::string& MTM::curvename() const {
  // @@protoc_insertion_point(field_get:FlumaionQL.MTM.curvename)
  return _internal_curvename();
}
inline void MTM::set_curvename(const std::string& value) {
  _internal_set_curvename(value);
  // @@protoc_insertion_point(field_set:FlumaionQL.MTM.curvename)
}
inline std::string* MTM::mutable_curvename() {
  // @@protoc_insertion_point(field_mutable:FlumaionQL.MTM.curvename)
  return _internal_mutable_curvename();
}
inline const std::string& MTM::_internal_curvename() const {
  return curvename_.GetNoArena();
}
inline void MTM::_internal_set_curvename(const std::string& value) {
  
  curvename_.SetNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), value);
}
inline void MTM::set_curvename(std::string&& value) {
  
  curvename_.SetNoArena(
    &::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::move(value));
  // @@protoc_insertion_point(field_set_rvalue:FlumaionQL.MTM.curvename)
}
inline void MTM::set_curvename(const char* value) {
  GOOGLE_DCHECK(value != nullptr);
  
  curvename_.SetNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::string(value));
  // @@protoc_insertion_point(field_set_char:FlumaionQL.MTM.curvename)
}
inline void MTM::set_curvename(const char* value, size_t size) {
  
  curvename_.SetNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      ::std::string(reinterpret_cast<const char*>(value), size));
  // @@protoc_insertion_point(field_set_pointer:FlumaionQL.MTM.curvename)
}
inline std::string* MTM::_internal_mutable_curvename() {
  
  return curvename_.MutableNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
}
inline std::string* MTM::release_curvename() {
  // @@protoc_insertion_point(field_release:FlumaionQL.MTM.curvename)
  
  return curvename_.ReleaseNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
}
inline void MTM::set_allocated_curvename(std::string* curvename) {
  if (curvename != nullptr) {
    
  } else {
    
  }
  curvename_.SetAllocatedNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), curvename);
  // @@protoc_insertion_point(field_set_allocated:FlumaionQL.MTM.curvename)
}

// repeated int64 fixlegdates = 3;
inline int MTM::_internal_fixlegdates_size() const {
  return fixlegdates_.size();
}
inline int MTM::fixlegdates_size() const {
  return _internal_fixlegdates_size();
}
inline void MTM::clear_fixlegdates() {
  fixlegdates_.Clear();
}
inline ::PROTOBUF_NAMESPACE_ID::int64 MTM::_internal_fixlegdates(int index) const {
  return fixlegdates_.Get(index);
}
inline ::PROTOBUF_NAMESPACE_ID::int64 MTM::fixlegdates(int index) const {
  // @@protoc_insertion_point(field_get:FlumaionQL.MTM.fixlegdates)
  return _internal_fixlegdates(index);
}
inline void MTM::set_fixlegdates(int index, ::PROTOBUF_NAMESPACE_ID::int64 value) {
  fixlegdates_.Set(index, value);
  // @@protoc_insertion_point(field_set:FlumaionQL.MTM.fixlegdates)
}
inline void MTM::_internal_add_fixlegdates(::PROTOBUF_NAMESPACE_ID::int64 value) {
  fixlegdates_.Add(value);
}
inline void MTM::add_fixlegdates(::PROTOBUF_NAMESPACE_ID::int64 value) {
  _internal_add_fixlegdates(value);
  // @@protoc_insertion_point(field_add:FlumaionQL.MTM.fixlegdates)
}
inline const ::PROTOBUF_NAMESPACE_ID::RepeatedField< ::PROTOBUF_NAMESPACE_ID::int64 >&
MTM::_internal_fixlegdates() const {
  return fixlegdates_;
}
inline const ::PROTOBUF_NAMESPACE_ID::RepeatedField< ::PROTOBUF_NAMESPACE_ID::int64 >&
MTM::fixlegdates() const {
  // @@protoc_insertion_point(field_list:FlumaionQL.MTM.fixlegdates)
  return _internal_fixlegdates();
}
inline ::PROTOBUF_NAMESPACE_ID::RepeatedField< ::PROTOBUF_NAMESPACE_ID::int64 >*
MTM::_internal_mutable_fixlegdates() {
  return &fixlegdates_;
}
inline ::PROTOBUF_NAMESPACE_ID::RepeatedField< ::PROTOBUF_NAMESPACE_ID::int64 >*
MTM::mutable_fixlegdates() {
  // @@protoc_insertion_point(field_mutable_list:FlumaionQL.MTM.fixlegdates)
  return _internal_mutable_fixlegdates();
}

// repeated float fixlegamount = 4;
inline int MTM::_internal_fixlegamount_size() const {
  return fixlegamount_.size();
}
inline int MTM::fixlegamount_size() const {
  return _internal_fixlegamount_size();
}
inline void MTM::clear_fixlegamount() {
  fixlegamount_.Clear();
}
inline float MTM::_internal_fixlegamount(int index) const {
  return fixlegamount_.Get(index);
}
inline float MTM::fixlegamount(int index) const {
  // @@protoc_insertion_point(field_get:FlumaionQL.MTM.fixlegamount)
  return _internal_fixlegamount(index);
}
inline void MTM::set_fixlegamount(int index, float value) {
  fixlegamount_.Set(index, value);
  // @@protoc_insertion_point(field_set:FlumaionQL.MTM.fixlegamount)
}
inline void MTM::_internal_add_fixlegamount(float value) {
  fixlegamount_.Add(value);
}
inline void MTM::add_fixlegamount(float value) {
  _internal_add_fixlegamount(value);
  // @@protoc_insertion_point(field_add:FlumaionQL.MTM.fixlegamount)
}
inline const ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >&
MTM::_internal_fixlegamount() const {
  return fixlegamount_;
}
inline const ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >&
MTM::fixlegamount() const {
  // @@protoc_insertion_point(field_list:FlumaionQL.MTM.fixlegamount)
  return _internal_fixlegamount();
}
inline ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >*
MTM::_internal_mutable_fixlegamount() {
  return &fixlegamount_;
}
inline ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >*
MTM::mutable_fixlegamount() {
  // @@protoc_insertion_point(field_mutable_list:FlumaionQL.MTM.fixlegamount)
  return _internal_mutable_fixlegamount();
}

// repeated int64 fltlegdates = 5;
inline int MTM::_internal_fltlegdates_size() const {
  return fltlegdates_.size();
}
inline int MTM::fltlegdates_size() const {
  return _internal_fltlegdates_size();
}
inline void MTM::clear_fltlegdates() {
  fltlegdates_.Clear();
}
inline ::PROTOBUF_NAMESPACE_ID::int64 MTM::_internal_fltlegdates(int index) const {
  return fltlegdates_.Get(index);
}
inline ::PROTOBUF_NAMESPACE_ID::int64 MTM::fltlegdates(int index) const {
  // @@protoc_insertion_point(field_get:FlumaionQL.MTM.fltlegdates)
  return _internal_fltlegdates(index);
}
inline void MTM::set_fltlegdates(int index, ::PROTOBUF_NAMESPACE_ID::int64 value) {
  fltlegdates_.Set(index, value);
  // @@protoc_insertion_point(field_set:FlumaionQL.MTM.fltlegdates)
}
inline void MTM::_internal_add_fltlegdates(::PROTOBUF_NAMESPACE_ID::int64 value) {
  fltlegdates_.Add(value);
}
inline void MTM::add_fltlegdates(::PROTOBUF_NAMESPACE_ID::int64 value) {
  _internal_add_fltlegdates(value);
  // @@protoc_insertion_point(field_add:FlumaionQL.MTM.fltlegdates)
}
inline const ::PROTOBUF_NAMESPACE_ID::RepeatedField< ::PROTOBUF_NAMESPACE_ID::int64 >&
MTM::_internal_fltlegdates() const {
  return fltlegdates_;
}
inline const ::PROTOBUF_NAMESPACE_ID::RepeatedField< ::PROTOBUF_NAMESPACE_ID::int64 >&
MTM::fltlegdates() const {
  // @@protoc_insertion_point(field_list:FlumaionQL.MTM.fltlegdates)
  return _internal_fltlegdates();
}
inline ::PROTOBUF_NAMESPACE_ID::RepeatedField< ::PROTOBUF_NAMESPACE_ID::int64 >*
MTM::_internal_mutable_fltlegdates() {
  return &fltlegdates_;
}
inline ::PROTOBUF_NAMESPACE_ID::RepeatedField< ::PROTOBUF_NAMESPACE_ID::int64 >*
MTM::mutable_fltlegdates() {
  // @@protoc_insertion_point(field_mutable_list:FlumaionQL.MTM.fltlegdates)
  return _internal_mutable_fltlegdates();
}

// repeated float fltlegamount = 6;
inline int MTM::_internal_fltlegamount_size() const {
  return fltlegamount_.size();
}
inline int MTM::fltlegamount_size() const {
  return _internal_fltlegamount_size();
}
inline void MTM::clear_fltlegamount() {
  fltlegamount_.Clear();
}
inline float MTM::_internal_fltlegamount(int index) const {
  return fltlegamount_.Get(index);
}
inline float MTM::fltlegamount(int index) const {
  // @@protoc_insertion_point(field_get:FlumaionQL.MTM.fltlegamount)
  return _internal_fltlegamount(index);
}
inline void MTM::set_fltlegamount(int index, float value) {
  fltlegamount_.Set(index, value);
  // @@protoc_insertion_point(field_set:FlumaionQL.MTM.fltlegamount)
}
inline void MTM::_internal_add_fltlegamount(float value) {
  fltlegamount_.Add(value);
}
inline void MTM::add_fltlegamount(float value) {
  _internal_add_fltlegamount(value);
  // @@protoc_insertion_point(field_add:FlumaionQL.MTM.fltlegamount)
}
inline const ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >&
MTM::_internal_fltlegamount() const {
  return fltlegamount_;
}
inline const ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >&
MTM::fltlegamount() const {
  // @@protoc_insertion_point(field_list:FlumaionQL.MTM.fltlegamount)
  return _internal_fltlegamount();
}
inline ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >*
MTM::_internal_mutable_fltlegamount() {
  return &fltlegamount_;
}
inline ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >*
MTM::mutable_fltlegamount() {
  // @@protoc_insertion_point(field_mutable_list:FlumaionQL.MTM.fltlegamount)
  return _internal_mutable_fltlegamount();
}

// repeated float discountvalues = 7;
inline int MTM::_internal_discountvalues_size() const {
  return discountvalues_.size();
}
inline int MTM::discountvalues_size() const {
  return _internal_discountvalues_size();
}
inline void MTM::clear_discountvalues() {
  discountvalues_.Clear();
}
inline float MTM::_internal_discountvalues(int index) const {
  return discountvalues_.Get(index);
}
inline float MTM::discountvalues(int index) const {
  // @@protoc_insertion_point(field_get:FlumaionQL.MTM.discountvalues)
  return _internal_discountvalues(index);
}
inline void MTM::set_discountvalues(int index, float value) {
  discountvalues_.Set(index, value);
  // @@protoc_insertion_point(field_set:FlumaionQL.MTM.discountvalues)
}
inline void MTM::_internal_add_discountvalues(float value) {
  discountvalues_.Add(value);
}
inline void MTM::add_discountvalues(float value) {
  _internal_add_discountvalues(value);
  // @@protoc_insertion_point(field_add:FlumaionQL.MTM.discountvalues)
}
inline const ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >&
MTM::_internal_discountvalues() const {
  return discountvalues_;
}
inline const ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >&
MTM::discountvalues() const {
  // @@protoc_insertion_point(field_list:FlumaionQL.MTM.discountvalues)
  return _internal_discountvalues();
}
inline ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >*
MTM::_internal_mutable_discountvalues() {
  return &discountvalues_;
}
inline ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >*
MTM::mutable_discountvalues() {
  // @@protoc_insertion_point(field_mutable_list:FlumaionQL.MTM.discountvalues)
  return _internal_mutable_discountvalues();
}

// repeated float legfractions = 8;
inline int MTM::_internal_legfractions_size() const {
  return legfractions_.size();
}
inline int MTM::legfractions_size() const {
  return _internal_legfractions_size();
}
inline void MTM::clear_legfractions() {
  legfractions_.Clear();
}
inline float MTM::_internal_legfractions(int index) const {
  return legfractions_.Get(index);
}
inline float MTM::legfractions(int index) const {
  // @@protoc_insertion_point(field_get:FlumaionQL.MTM.legfractions)
  return _internal_legfractions(index);
}
inline void MTM::set_legfractions(int index, float value) {
  legfractions_.Set(index, value);
  // @@protoc_insertion_point(field_set:FlumaionQL.MTM.legfractions)
}
inline void MTM::_internal_add_legfractions(float value) {
  legfractions_.Add(value);
}
inline void MTM::add_legfractions(float value) {
  _internal_add_legfractions(value);
  // @@protoc_insertion_point(field_add:FlumaionQL.MTM.legfractions)
}
inline const ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >&
MTM::_internal_legfractions() const {
  return legfractions_;
}
inline const ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >&
MTM::legfractions() const {
  // @@protoc_insertion_point(field_list:FlumaionQL.MTM.legfractions)
  return _internal_legfractions();
}
inline ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >*
MTM::_internal_mutable_legfractions() {
  return &legfractions_;
}
inline ::PROTOBUF_NAMESPACE_ID::RepeatedField< float >*
MTM::mutable_legfractions() {
  // @@protoc_insertion_point(field_mutable_list:FlumaionQL.MTM.legfractions)
  return _internal_mutable_legfractions();
}

// bool haserrored = 9;
inline void MTM::clear_haserrored() {
  haserrored_ = false;
}
inline bool MTM::_internal_haserrored() const {
  return haserrored_;
}
inline bool MTM::haserrored() const {
  // @@protoc_insertion_point(field_get:FlumaionQL.MTM.haserrored)
  return _internal_haserrored();
}
inline void MTM::_internal_set_haserrored(bool value) {
  
  haserrored_ = value;
}
inline void MTM::set_haserrored(bool value) {
  _internal_set_haserrored(value);
  // @@protoc_insertion_point(field_set:FlumaionQL.MTM.haserrored)
}

// string error = 10;
inline void MTM::clear_error() {
  error_.ClearToEmptyNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
}
inline const std::string& MTM::error() const {
  // @@protoc_insertion_point(field_get:FlumaionQL.MTM.error)
  return _internal_error();
}
inline void MTM::set_error(const std::string& value) {
  _internal_set_error(value);
  // @@protoc_insertion_point(field_set:FlumaionQL.MTM.error)
}
inline std::string* MTM::mutable_error() {
  // @@protoc_insertion_point(field_mutable:FlumaionQL.MTM.error)
  return _internal_mutable_error();
}
inline const std::string& MTM::_internal_error() const {
  return error_.GetNoArena();
}
inline void MTM::_internal_set_error(const std::string& value) {
  
  error_.SetNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), value);
}
inline void MTM::set_error(std::string&& value) {
  
  error_.SetNoArena(
    &::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::move(value));
  // @@protoc_insertion_point(field_set_rvalue:FlumaionQL.MTM.error)
}
inline void MTM::set_error(const char* value) {
  GOOGLE_DCHECK(value != nullptr);
  
  error_.SetNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::string(value));
  // @@protoc_insertion_point(field_set_char:FlumaionQL.MTM.error)
}
inline void MTM::set_error(const char* value, size_t size) {
  
  error_.SetNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      ::std::string(reinterpret_cast<const char*>(value), size));
  // @@protoc_insertion_point(field_set_pointer:FlumaionQL.MTM.error)
}
inline std::string* MTM::_internal_mutable_error() {
  
  return error_.MutableNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
}
inline std::string* MTM::release_error() {
  // @@protoc_insertion_point(field_release:FlumaionQL.MTM.error)
  
  return error_.ReleaseNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
}
inline void MTM::set_allocated_error(std::string* error) {
  if (error != nullptr) {
    
  } else {
    
  }
  error_.SetAllocatedNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), error);
  // @@protoc_insertion_point(field_set_allocated:FlumaionQL.MTM.error)
}

// int64 computetimemicros = 11;
inline void MTM::clear_computetimemicros() {
  computetimemicros_ = PROTOBUF_LONGLONG(0);
}
inline ::PROTOBUF_NAMESPACE_ID::int64 MTM::_internal_computetimemicros() const {
  return computetimemicros_;
}
inline ::PROTOBUF_NAMESPACE_ID::int64 MTM::computetimemicros() const {
  // @@protoc_insertion_point(field_get:FlumaionQL.MTM.computetimemicros)
  return _internal_computetimemicros();
}
inline void MTM::_internal_set_computetimemicros(::PROTOBUF_NAMESPACE_ID::int64 value) {
  
  computetimemicros_ = value;
}
inline void MTM::set_computetimemicros(::PROTOBUF_NAMESPACE_ID::int64 value) {
  _internal_set_computetimemicros(value);
  // @@protoc_insertion_point(field_set:FlumaionQL.MTM.computetimemicros)
}

#ifdef __GNUC__
  #pragma GCC diagnostic pop
#endif  // __GNUC__

// @@protoc_insertion_point(namespace_scope)

}  // namespace FlumaionQL

// @@protoc_insertion_point(global_scope)

#include <google/protobuf/port_undef.inc>
#endif  // GOOGLE_PROTOBUF_INCLUDED_GOOGLE_PROTOBUF_INCLUDED_mtms_2eproto