// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: curve.proto

#include "../include/curve.pb.h"

#include <algorithm>

#include <google/protobuf/io/coded_stream.h>
#include <google/protobuf/extension_set.h>
#include <google/protobuf/wire_format_lite.h>
#include <google/protobuf/descriptor.h>
#include <google/protobuf/generated_message_reflection.h>
#include <google/protobuf/reflection_ops.h>
#include <google/protobuf/wire_format.h>
// @@protoc_insertion_point(includes)
#include <google/protobuf/port_def.inc>
namespace FlumaionQL {
class CurveDefaultTypeInternal {
 public:
  ::PROTOBUF_NAMESPACE_ID::internal::ExplicitlyConstructed<Curve> _instance;
} _Curve_default_instance_;
}  // namespace FlumaionQL
static void InitDefaultsscc_info_Curve_curve_2eproto() {
  GOOGLE_PROTOBUF_VERIFY_VERSION;

  {
    void* ptr = &::FlumaionQL::_Curve_default_instance_;
    new (ptr) ::FlumaionQL::Curve();
    ::PROTOBUF_NAMESPACE_ID::internal::OnShutdownDestroyMessage(ptr);
  }
  ::FlumaionQL::Curve::InitAsDefaultInstance();
}

::PROTOBUF_NAMESPACE_ID::internal::SCCInfo<0> scc_info_Curve_curve_2eproto =
    {{ATOMIC_VAR_INIT(::PROTOBUF_NAMESPACE_ID::internal::SCCInfoBase::kUninitialized), 0, 0, InitDefaultsscc_info_Curve_curve_2eproto}, {}};

static ::PROTOBUF_NAMESPACE_ID::Metadata file_level_metadata_curve_2eproto[1];
static constexpr ::PROTOBUF_NAMESPACE_ID::EnumDescriptor const** file_level_enum_descriptors_curve_2eproto = nullptr;
static constexpr ::PROTOBUF_NAMESPACE_ID::ServiceDescriptor const** file_level_service_descriptors_curve_2eproto = nullptr;

const ::PROTOBUF_NAMESPACE_ID::uint32 TableStruct_curve_2eproto::offsets[] PROTOBUF_SECTION_VARIABLE(protodesc_cold) = {
  ~0u,  // no _has_bits_
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::Curve, _internal_metadata_),
  ~0u,  // no _extensions_
  ~0u,  // no _oneof_case_
  ~0u,  // no _weak_field_map_
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::Curve, curvename_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::Curve, index_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::Curve, index_frequency_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::Curve, index_frequency_type_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::Curve, calendar_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::Curve, bussiness_convention_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::Curve, dcc_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::Curve, end_of_month_flag_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::Curve, settlement_days_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::Curve, maturity_period_value_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::Curve, maturity_period_type_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::Curve, rates_),
};
static const ::PROTOBUF_NAMESPACE_ID::internal::MigrationSchema schemas[] PROTOBUF_SECTION_VARIABLE(protodesc_cold) = {
  { 0, -1, sizeof(::FlumaionQL::Curve)},
};

static ::PROTOBUF_NAMESPACE_ID::Message const * const file_default_instances[] = {
  reinterpret_cast<const ::PROTOBUF_NAMESPACE_ID::Message*>(&::FlumaionQL::_Curve_default_instance_),
};

const char descriptor_table_protodef_curve_2eproto[] PROTOBUF_SECTION_VARIABLE(protodesc_cold) =
  "\n\013curve.proto\022\nFlumaionQL\"\235\002\n\005Curve\022\021\n\tc"
  "urvename\030\001 \001(\t\022\r\n\005index\030\002 \001(\t\022\027\n\017index_f"
  "requency\030\003 \001(\005\022\034\n\024index_frequency_type\030\004"
  " \001(\003\022\020\n\010calendar\030\005 \001(\t\022\034\n\024bussiness_conv"
  "ention\030\006 \001(\005\022\013\n\003dcc\030\007 \001(\t\022\031\n\021end_of_mont"
  "h_flag\030\010 \001(\010\022\027\n\017settlement_days\030\t \001(\005\022\035\n"
  "\025maturity_period_value\030\n \003(\005\022\034\n\024maturity"
  "_period_type\030\013 \003(\005\022\r\n\005rates\030\014 \003(\002b\006proto"
  "3"
  ;
static const ::PROTOBUF_NAMESPACE_ID::internal::DescriptorTable*const descriptor_table_curve_2eproto_deps[1] = {
};
static ::PROTOBUF_NAMESPACE_ID::internal::SCCInfoBase*const descriptor_table_curve_2eproto_sccs[1] = {
  &scc_info_Curve_curve_2eproto.base,
};
static ::PROTOBUF_NAMESPACE_ID::internal::once_flag descriptor_table_curve_2eproto_once;
const ::PROTOBUF_NAMESPACE_ID::internal::DescriptorTable descriptor_table_curve_2eproto = {
  false, false, descriptor_table_protodef_curve_2eproto, "curve.proto", 321,
  &descriptor_table_curve_2eproto_once, descriptor_table_curve_2eproto_sccs, descriptor_table_curve_2eproto_deps, 1, 0,
  schemas, file_default_instances, TableStruct_curve_2eproto::offsets,
  file_level_metadata_curve_2eproto, 1, file_level_enum_descriptors_curve_2eproto, file_level_service_descriptors_curve_2eproto,
};

// Force running AddDescriptors() at dynamic initialization time.
static bool dynamic_init_dummy_curve_2eproto = (static_cast<void>(::PROTOBUF_NAMESPACE_ID::internal::AddDescriptors(&descriptor_table_curve_2eproto)), true);
namespace FlumaionQL {

// ===================================================================

void Curve::InitAsDefaultInstance() {
}
class Curve::_Internal {
 public:
};

Curve::Curve(::PROTOBUF_NAMESPACE_ID::Arena* arena)
  : ::PROTOBUF_NAMESPACE_ID::Message(arena),
  maturity_period_value_(arena),
  maturity_period_type_(arena),
  rates_(arena) {
  SharedCtor();
  RegisterArenaDtor(arena);
  // @@protoc_insertion_point(arena_constructor:FlumaionQL.Curve)
}
Curve::Curve(const Curve& from)
  : ::PROTOBUF_NAMESPACE_ID::Message(),
      maturity_period_value_(from.maturity_period_value_),
      maturity_period_type_(from.maturity_period_type_),
      rates_(from.rates_) {
  _internal_metadata_.MergeFrom<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(from._internal_metadata_);
  curvename_.UnsafeSetDefault(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
  if (!from._internal_curvename().empty()) {
    curvename_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), from._internal_curvename(),
      GetArena());
  }
  index_.UnsafeSetDefault(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
  if (!from._internal_index().empty()) {
    index_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), from._internal_index(),
      GetArena());
  }
  calendar_.UnsafeSetDefault(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
  if (!from._internal_calendar().empty()) {
    calendar_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), from._internal_calendar(),
      GetArena());
  }
  dcc_.UnsafeSetDefault(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
  if (!from._internal_dcc().empty()) {
    dcc_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), from._internal_dcc(),
      GetArena());
  }
  ::memcpy(&index_frequency_type_, &from.index_frequency_type_,
    static_cast<size_t>(reinterpret_cast<char*>(&settlement_days_) -
    reinterpret_cast<char*>(&index_frequency_type_)) + sizeof(settlement_days_));
  // @@protoc_insertion_point(copy_constructor:FlumaionQL.Curve)
}

void Curve::SharedCtor() {
  ::PROTOBUF_NAMESPACE_ID::internal::InitSCC(&scc_info_Curve_curve_2eproto.base);
  curvename_.UnsafeSetDefault(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
  index_.UnsafeSetDefault(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
  calendar_.UnsafeSetDefault(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
  dcc_.UnsafeSetDefault(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
  ::memset(&index_frequency_type_, 0, static_cast<size_t>(
      reinterpret_cast<char*>(&settlement_days_) -
      reinterpret_cast<char*>(&index_frequency_type_)) + sizeof(settlement_days_));
}

Curve::~Curve() {
  // @@protoc_insertion_point(destructor:FlumaionQL.Curve)
  SharedDtor();
  _internal_metadata_.Delete<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>();
}

void Curve::SharedDtor() {
  GOOGLE_DCHECK(GetArena() == nullptr);
  curvename_.DestroyNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
  index_.DestroyNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
  calendar_.DestroyNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
  dcc_.DestroyNoArena(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited());
}

void Curve::ArenaDtor(void* object) {
  Curve* _this = reinterpret_cast< Curve* >(object);
  (void)_this;
}
void Curve::RegisterArenaDtor(::PROTOBUF_NAMESPACE_ID::Arena*) {
}
void Curve::SetCachedSize(int size) const {
  _cached_size_.Set(size);
}
const Curve& Curve::default_instance() {
  ::PROTOBUF_NAMESPACE_ID::internal::InitSCC(&::scc_info_Curve_curve_2eproto.base);
  return *internal_default_instance();
}


void Curve::Clear() {
// @@protoc_insertion_point(message_clear_start:FlumaionQL.Curve)
  ::PROTOBUF_NAMESPACE_ID::uint32 cached_has_bits = 0;
  // Prevent compiler warnings about cached_has_bits being unused
  (void) cached_has_bits;

  maturity_period_value_.Clear();
  maturity_period_type_.Clear();
  rates_.Clear();
  curvename_.ClearToEmpty(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
  index_.ClearToEmpty(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
  calendar_.ClearToEmpty(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
  dcc_.ClearToEmpty(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
  ::memset(&index_frequency_type_, 0, static_cast<size_t>(
      reinterpret_cast<char*>(&settlement_days_) -
      reinterpret_cast<char*>(&index_frequency_type_)) + sizeof(settlement_days_));
  _internal_metadata_.Clear<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>();
}

const char* Curve::_InternalParse(const char* ptr, ::PROTOBUF_NAMESPACE_ID::internal::ParseContext* ctx) {
#define CHK_(x) if (PROTOBUF_PREDICT_FALSE(!(x))) goto failure
  ::PROTOBUF_NAMESPACE_ID::Arena* arena = GetArena(); (void)arena;
  while (!ctx->Done(&ptr)) {
    ::PROTOBUF_NAMESPACE_ID::uint32 tag;
    ptr = ::PROTOBUF_NAMESPACE_ID::internal::ReadTag(ptr, &tag);
    CHK_(ptr);
    switch (tag >> 3) {
      // string curvename = 1;
      case 1:
        if (PROTOBUF_PREDICT_TRUE(static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 10)) {
          auto str = _internal_mutable_curvename();
          ptr = ::PROTOBUF_NAMESPACE_ID::internal::InlineGreedyStringParser(str, ptr, ctx);
          CHK_(::PROTOBUF_NAMESPACE_ID::internal::VerifyUTF8(str, "FlumaionQL.Curve.curvename"));
          CHK_(ptr);
        } else goto handle_unusual;
        continue;
      // string index = 2;
      case 2:
        if (PROTOBUF_PREDICT_TRUE(static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 18)) {
          auto str = _internal_mutable_index();
          ptr = ::PROTOBUF_NAMESPACE_ID::internal::InlineGreedyStringParser(str, ptr, ctx);
          CHK_(::PROTOBUF_NAMESPACE_ID::internal::VerifyUTF8(str, "FlumaionQL.Curve.index"));
          CHK_(ptr);
        } else goto handle_unusual;
        continue;
      // int32 index_frequency = 3;
      case 3:
        if (PROTOBUF_PREDICT_TRUE(static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 24)) {
          index_frequency_ = ::PROTOBUF_NAMESPACE_ID::internal::ReadVarint64(&ptr);
          CHK_(ptr);
        } else goto handle_unusual;
        continue;
      // int64 index_frequency_type = 4;
      case 4:
        if (PROTOBUF_PREDICT_TRUE(static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 32)) {
          index_frequency_type_ = ::PROTOBUF_NAMESPACE_ID::internal::ReadVarint64(&ptr);
          CHK_(ptr);
        } else goto handle_unusual;
        continue;
      // string calendar = 5;
      case 5:
        if (PROTOBUF_PREDICT_TRUE(static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 42)) {
          auto str = _internal_mutable_calendar();
          ptr = ::PROTOBUF_NAMESPACE_ID::internal::InlineGreedyStringParser(str, ptr, ctx);
          CHK_(::PROTOBUF_NAMESPACE_ID::internal::VerifyUTF8(str, "FlumaionQL.Curve.calendar"));
          CHK_(ptr);
        } else goto handle_unusual;
        continue;
      // int32 bussiness_convention = 6;
      case 6:
        if (PROTOBUF_PREDICT_TRUE(static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 48)) {
          bussiness_convention_ = ::PROTOBUF_NAMESPACE_ID::internal::ReadVarint64(&ptr);
          CHK_(ptr);
        } else goto handle_unusual;
        continue;
      // string dcc = 7;
      case 7:
        if (PROTOBUF_PREDICT_TRUE(static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 58)) {
          auto str = _internal_mutable_dcc();
          ptr = ::PROTOBUF_NAMESPACE_ID::internal::InlineGreedyStringParser(str, ptr, ctx);
          CHK_(::PROTOBUF_NAMESPACE_ID::internal::VerifyUTF8(str, "FlumaionQL.Curve.dcc"));
          CHK_(ptr);
        } else goto handle_unusual;
        continue;
      // bool end_of_month_flag = 8;
      case 8:
        if (PROTOBUF_PREDICT_TRUE(static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 64)) {
          end_of_month_flag_ = ::PROTOBUF_NAMESPACE_ID::internal::ReadVarint64(&ptr);
          CHK_(ptr);
        } else goto handle_unusual;
        continue;
      // int32 settlement_days = 9;
      case 9:
        if (PROTOBUF_PREDICT_TRUE(static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 72)) {
          settlement_days_ = ::PROTOBUF_NAMESPACE_ID::internal::ReadVarint64(&ptr);
          CHK_(ptr);
        } else goto handle_unusual;
        continue;
      // repeated int32 maturity_period_value = 10;
      case 10:
        if (PROTOBUF_PREDICT_TRUE(static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 82)) {
          ptr = ::PROTOBUF_NAMESPACE_ID::internal::PackedInt32Parser(_internal_mutable_maturity_period_value(), ptr, ctx);
          CHK_(ptr);
        } else if (static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 80) {
          _internal_add_maturity_period_value(::PROTOBUF_NAMESPACE_ID::internal::ReadVarint64(&ptr));
          CHK_(ptr);
        } else goto handle_unusual;
        continue;
      // repeated int32 maturity_period_type = 11;
      case 11:
        if (PROTOBUF_PREDICT_TRUE(static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 90)) {
          ptr = ::PROTOBUF_NAMESPACE_ID::internal::PackedInt32Parser(_internal_mutable_maturity_period_type(), ptr, ctx);
          CHK_(ptr);
        } else if (static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 88) {
          _internal_add_maturity_period_type(::PROTOBUF_NAMESPACE_ID::internal::ReadVarint64(&ptr));
          CHK_(ptr);
        } else goto handle_unusual;
        continue;
      // repeated float rates = 12;
      case 12:
        if (PROTOBUF_PREDICT_TRUE(static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 98)) {
          ptr = ::PROTOBUF_NAMESPACE_ID::internal::PackedFloatParser(_internal_mutable_rates(), ptr, ctx);
          CHK_(ptr);
        } else if (static_cast<::PROTOBUF_NAMESPACE_ID::uint8>(tag) == 101) {
          _internal_add_rates(::PROTOBUF_NAMESPACE_ID::internal::UnalignedLoad<float>(ptr));
          ptr += sizeof(float);
        } else goto handle_unusual;
        continue;
      default: {
      handle_unusual:
        if ((tag & 7) == 4 || tag == 0) {
          ctx->SetLastTag(tag);
          goto success;
        }
        ptr = UnknownFieldParse(tag,
            _internal_metadata_.mutable_unknown_fields<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(),
            ptr, ctx);
        CHK_(ptr != nullptr);
        continue;
      }
    }  // switch
  }  // while
success:
  return ptr;
failure:
  ptr = nullptr;
  goto success;
#undef CHK_
}

::PROTOBUF_NAMESPACE_ID::uint8* Curve::_InternalSerialize(
    ::PROTOBUF_NAMESPACE_ID::uint8* target, ::PROTOBUF_NAMESPACE_ID::io::EpsCopyOutputStream* stream) const {
  // @@protoc_insertion_point(serialize_to_array_start:FlumaionQL.Curve)
  ::PROTOBUF_NAMESPACE_ID::uint32 cached_has_bits = 0;
  (void) cached_has_bits;

  // string curvename = 1;
  if (this->curvename().size() > 0) {
    ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::VerifyUtf8String(
      this->_internal_curvename().data(), static_cast<int>(this->_internal_curvename().length()),
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::SERIALIZE,
      "FlumaionQL.Curve.curvename");
    target = stream->WriteStringMaybeAliased(
        1, this->_internal_curvename(), target);
  }

  // string index = 2;
  if (this->index().size() > 0) {
    ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::VerifyUtf8String(
      this->_internal_index().data(), static_cast<int>(this->_internal_index().length()),
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::SERIALIZE,
      "FlumaionQL.Curve.index");
    target = stream->WriteStringMaybeAliased(
        2, this->_internal_index(), target);
  }

  // int32 index_frequency = 3;
  if (this->index_frequency() != 0) {
    target = stream->EnsureSpace(target);
    target = ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::WriteInt32ToArray(3, this->_internal_index_frequency(), target);
  }

  // int64 index_frequency_type = 4;
  if (this->index_frequency_type() != 0) {
    target = stream->EnsureSpace(target);
    target = ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::WriteInt64ToArray(4, this->_internal_index_frequency_type(), target);
  }

  // string calendar = 5;
  if (this->calendar().size() > 0) {
    ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::VerifyUtf8String(
      this->_internal_calendar().data(), static_cast<int>(this->_internal_calendar().length()),
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::SERIALIZE,
      "FlumaionQL.Curve.calendar");
    target = stream->WriteStringMaybeAliased(
        5, this->_internal_calendar(), target);
  }

  // int32 bussiness_convention = 6;
  if (this->bussiness_convention() != 0) {
    target = stream->EnsureSpace(target);
    target = ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::WriteInt32ToArray(6, this->_internal_bussiness_convention(), target);
  }

  // string dcc = 7;
  if (this->dcc().size() > 0) {
    ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::VerifyUtf8String(
      this->_internal_dcc().data(), static_cast<int>(this->_internal_dcc().length()),
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::SERIALIZE,
      "FlumaionQL.Curve.dcc");
    target = stream->WriteStringMaybeAliased(
        7, this->_internal_dcc(), target);
  }

  // bool end_of_month_flag = 8;
  if (this->end_of_month_flag() != 0) {
    target = stream->EnsureSpace(target);
    target = ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::WriteBoolToArray(8, this->_internal_end_of_month_flag(), target);
  }

  // int32 settlement_days = 9;
  if (this->settlement_days() != 0) {
    target = stream->EnsureSpace(target);
    target = ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::WriteInt32ToArray(9, this->_internal_settlement_days(), target);
  }

  // repeated int32 maturity_period_value = 10;
  {
    int byte_size = _maturity_period_value_cached_byte_size_.load(std::memory_order_relaxed);
    if (byte_size > 0) {
      target = stream->WriteInt32Packed(
          10, _internal_maturity_period_value(), byte_size, target);
    }
  }

  // repeated int32 maturity_period_type = 11;
  {
    int byte_size = _maturity_period_type_cached_byte_size_.load(std::memory_order_relaxed);
    if (byte_size > 0) {
      target = stream->WriteInt32Packed(
          11, _internal_maturity_period_type(), byte_size, target);
    }
  }

  // repeated float rates = 12;
  if (this->_internal_rates_size() > 0) {
    target = stream->WriteFixedPacked(12, _internal_rates(), target);
  }

  if (PROTOBUF_PREDICT_FALSE(_internal_metadata_.have_unknown_fields())) {
    target = ::PROTOBUF_NAMESPACE_ID::internal::WireFormat::InternalSerializeUnknownFieldsToArray(
        _internal_metadata_.unknown_fields<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(::PROTOBUF_NAMESPACE_ID::UnknownFieldSet::default_instance), target, stream);
  }
  // @@protoc_insertion_point(serialize_to_array_end:FlumaionQL.Curve)
  return target;
}

size_t Curve::ByteSizeLong() const {
// @@protoc_insertion_point(message_byte_size_start:FlumaionQL.Curve)
  size_t total_size = 0;

  ::PROTOBUF_NAMESPACE_ID::uint32 cached_has_bits = 0;
  // Prevent compiler warnings about cached_has_bits being unused
  (void) cached_has_bits;

  // repeated int32 maturity_period_value = 10;
  {
    size_t data_size = ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::
      Int32Size(this->maturity_period_value_);
    if (data_size > 0) {
      total_size += 1 +
        ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::Int32Size(
            static_cast<::PROTOBUF_NAMESPACE_ID::int32>(data_size));
    }
    int cached_size = ::PROTOBUF_NAMESPACE_ID::internal::ToCachedSize(data_size);
    _maturity_period_value_cached_byte_size_.store(cached_size,
                                    std::memory_order_relaxed);
    total_size += data_size;
  }

  // repeated int32 maturity_period_type = 11;
  {
    size_t data_size = ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::
      Int32Size(this->maturity_period_type_);
    if (data_size > 0) {
      total_size += 1 +
        ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::Int32Size(
            static_cast<::PROTOBUF_NAMESPACE_ID::int32>(data_size));
    }
    int cached_size = ::PROTOBUF_NAMESPACE_ID::internal::ToCachedSize(data_size);
    _maturity_period_type_cached_byte_size_.store(cached_size,
                                    std::memory_order_relaxed);
    total_size += data_size;
  }

  // repeated float rates = 12;
  {
    unsigned int count = static_cast<unsigned int>(this->_internal_rates_size());
    size_t data_size = 4UL * count;
    if (data_size > 0) {
      total_size += 1 +
        ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::Int32Size(
            static_cast<::PROTOBUF_NAMESPACE_ID::int32>(data_size));
    }
    int cached_size = ::PROTOBUF_NAMESPACE_ID::internal::ToCachedSize(data_size);
    _rates_cached_byte_size_.store(cached_size,
                                    std::memory_order_relaxed);
    total_size += data_size;
  }

  // string curvename = 1;
  if (this->curvename().size() > 0) {
    total_size += 1 +
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::StringSize(
        this->_internal_curvename());
  }

  // string index = 2;
  if (this->index().size() > 0) {
    total_size += 1 +
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::StringSize(
        this->_internal_index());
  }

  // string calendar = 5;
  if (this->calendar().size() > 0) {
    total_size += 1 +
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::StringSize(
        this->_internal_calendar());
  }

  // string dcc = 7;
  if (this->dcc().size() > 0) {
    total_size += 1 +
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::StringSize(
        this->_internal_dcc());
  }

  // int64 index_frequency_type = 4;
  if (this->index_frequency_type() != 0) {
    total_size += 1 +
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::Int64Size(
        this->_internal_index_frequency_type());
  }

  // int32 index_frequency = 3;
  if (this->index_frequency() != 0) {
    total_size += 1 +
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::Int32Size(
        this->_internal_index_frequency());
  }

  // int32 bussiness_convention = 6;
  if (this->bussiness_convention() != 0) {
    total_size += 1 +
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::Int32Size(
        this->_internal_bussiness_convention());
  }

  // bool end_of_month_flag = 8;
  if (this->end_of_month_flag() != 0) {
    total_size += 1 + 1;
  }

  // int32 settlement_days = 9;
  if (this->settlement_days() != 0) {
    total_size += 1 +
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::Int32Size(
        this->_internal_settlement_days());
  }

  if (PROTOBUF_PREDICT_FALSE(_internal_metadata_.have_unknown_fields())) {
    return ::PROTOBUF_NAMESPACE_ID::internal::ComputeUnknownFieldsSize(
        _internal_metadata_, total_size, &_cached_size_);
  }
  int cached_size = ::PROTOBUF_NAMESPACE_ID::internal::ToCachedSize(total_size);
  SetCachedSize(cached_size);
  return total_size;
}

void Curve::MergeFrom(const ::PROTOBUF_NAMESPACE_ID::Message& from) {
// @@protoc_insertion_point(generalized_merge_from_start:FlumaionQL.Curve)
  GOOGLE_DCHECK_NE(&from, this);
  const Curve* source =
      ::PROTOBUF_NAMESPACE_ID::DynamicCastToGenerated<Curve>(
          &from);
  if (source == nullptr) {
  // @@protoc_insertion_point(generalized_merge_from_cast_fail:FlumaionQL.Curve)
    ::PROTOBUF_NAMESPACE_ID::internal::ReflectionOps::Merge(from, this);
  } else {
  // @@protoc_insertion_point(generalized_merge_from_cast_success:FlumaionQL.Curve)
    MergeFrom(*source);
  }
}

void Curve::MergeFrom(const Curve& from) {
// @@protoc_insertion_point(class_specific_merge_from_start:FlumaionQL.Curve)
  GOOGLE_DCHECK_NE(&from, this);
  _internal_metadata_.MergeFrom<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(from._internal_metadata_);
  ::PROTOBUF_NAMESPACE_ID::uint32 cached_has_bits = 0;
  (void) cached_has_bits;

  maturity_period_value_.MergeFrom(from.maturity_period_value_);
  maturity_period_type_.MergeFrom(from.maturity_period_type_);
  rates_.MergeFrom(from.rates_);
  if (from.curvename().size() > 0) {
    _internal_set_curvename(from._internal_curvename());
  }
  if (from.index().size() > 0) {
    _internal_set_index(from._internal_index());
  }
  if (from.calendar().size() > 0) {
    _internal_set_calendar(from._internal_calendar());
  }
  if (from.dcc().size() > 0) {
    _internal_set_dcc(from._internal_dcc());
  }
  if (from.index_frequency_type() != 0) {
    _internal_set_index_frequency_type(from._internal_index_frequency_type());
  }
  if (from.index_frequency() != 0) {
    _internal_set_index_frequency(from._internal_index_frequency());
  }
  if (from.bussiness_convention() != 0) {
    _internal_set_bussiness_convention(from._internal_bussiness_convention());
  }
  if (from.end_of_month_flag() != 0) {
    _internal_set_end_of_month_flag(from._internal_end_of_month_flag());
  }
  if (from.settlement_days() != 0) {
    _internal_set_settlement_days(from._internal_settlement_days());
  }
}

void Curve::CopyFrom(const ::PROTOBUF_NAMESPACE_ID::Message& from) {
// @@protoc_insertion_point(generalized_copy_from_start:FlumaionQL.Curve)
  if (&from == this) return;
  Clear();
  MergeFrom(from);
}

void Curve::CopyFrom(const Curve& from) {
// @@protoc_insertion_point(class_specific_copy_from_start:FlumaionQL.Curve)
  if (&from == this) return;
  Clear();
  MergeFrom(from);
}

bool Curve::IsInitialized() const {
  return true;
}

void Curve::InternalSwap(Curve* other) {
  using std::swap;
  _internal_metadata_.Swap<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(&other->_internal_metadata_);
  maturity_period_value_.InternalSwap(&other->maturity_period_value_);
  maturity_period_type_.InternalSwap(&other->maturity_period_type_);
  rates_.InternalSwap(&other->rates_);
  curvename_.Swap(&other->curvename_, &::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
  index_.Swap(&other->index_, &::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
  calendar_.Swap(&other->calendar_, &::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
  dcc_.Swap(&other->dcc_, &::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
  ::PROTOBUF_NAMESPACE_ID::internal::memswap<
      PROTOBUF_FIELD_OFFSET(Curve, settlement_days_)
      + sizeof(Curve::settlement_days_)
      - PROTOBUF_FIELD_OFFSET(Curve, index_frequency_type_)>(
          reinterpret_cast<char*>(&index_frequency_type_),
          reinterpret_cast<char*>(&other->index_frequency_type_));
}

::PROTOBUF_NAMESPACE_ID::Metadata Curve::GetMetadata() const {
  return GetMetadataStatic();
}


// @@protoc_insertion_point(namespace_scope)
}  // namespace FlumaionQL
PROTOBUF_NAMESPACE_OPEN
template<> PROTOBUF_NOINLINE ::FlumaionQL::Curve* Arena::CreateMaybeMessage< ::FlumaionQL::Curve >(Arena* arena) {
  return Arena::CreateMessageInternal< ::FlumaionQL::Curve >(arena);
}
PROTOBUF_NAMESPACE_CLOSE

// @@protoc_insertion_point(global_scope)
#include <google/protobuf/port_undef.inc>